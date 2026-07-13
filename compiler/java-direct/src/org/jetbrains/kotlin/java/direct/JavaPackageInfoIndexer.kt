/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.java.direct.model.JavaAnnotationOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap

/**
 * Parses `package-info.java` files and aggregates package-level [JavaAnnotation]s per package.
 *
 * Called by [JavaPackageIndexer] whenever a `package-info.java` is encountered during its
 * directory walk. Reads are deduplicated at the call site (each package is indexed at most once);
 * on the off chance multiple `package-info.java` files target the same package, their annotations
 * are merged.
 */
internal class JavaPackageInfoIndexer(
    private val sourceFileReader: JavaSourceFileReader,
    private val resolutionContextFactory: (JavaLightTree) -> JavaResolutionContext,
) {
    private val packageAnnotationNodes: ConcurrentHashMap<FqName, List<JavaAnnotation>> = ConcurrentHashMap()

    /**
     * Parses [file] and appends its package-level annotations to the per-package cache.
     *
     * @param expectedPackage When non-null, validates that the file's declared package matches.
     *   Used during directory-based lazy indexing to skip files with mismatched package/directory.
     *   When null (the file type source roots in init), any package is accepted.
     */
    fun indexPackageInfo(file: VirtualFile, expectedPackage: FqName?) {
        val source = sourceFileReader.readFileContent(file) ?: return
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()

        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = (packageStmt ?: root).let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        } ?: return
        val packageFqName = FqName(packageName)

        if (expectedPackage != null && packageFqName != expectedPackage) return

        val resolutionContext = resolutionContextFactory(tree)
        val annotations = mutableListOf<JavaAnnotation>()

        // Annotations are in PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION (KMP parser structure).
        // Also check other plausible locations for robustness.
        packageStmt?.let { ps ->
            tree.findChildByType(ps, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
            }
            tree.getChildrenByType(ps, JavaSyntaxElementType.ANNOTATION)
                .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }
        tree.getChildrenByType(root, JavaSyntaxElementType.ANNOTATION)
            .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        tree.findChildByType(root, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }

        if (annotations.isNotEmpty()) {
            packageAnnotationNodes.merge(packageFqName, annotations.toList()) { existing, new -> existing + new }
        }
    }

    fun getPackageAnnotations(packageFqName: FqName): List<JavaAnnotation> =
        packageAnnotationNodes[packageFqName] ?: emptyList()

    fun hasPackageAnnotations(packageFqName: FqName): Boolean =
        !packageAnnotationNodes[packageFqName].isNullOrEmpty()
}
