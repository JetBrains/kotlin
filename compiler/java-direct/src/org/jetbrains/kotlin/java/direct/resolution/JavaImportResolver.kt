/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.resolution

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Four-bucket holder for the imports of a Java compilation unit, mirroring the JLS 7.5
 * `ImportDeclaration` productions:
 * - [simpleTypeImports] — `import a.b.C;`
 * - [staticSingleImports] — `import static a.b.C.X;` (`X` may be a type, method, or field)
 * - [typeStarImports] — `import a.b.*;` (values are *packages*)
 * - [staticStarImports] — `import static a.b.C.*;` (values are *outer-class* FqNames)
 *
 * The JLS 6.4.1 shadowing ranks between the buckets are applied by the step functions in
 * `JavaTypeResolver.kt`.
 */
internal class JavaImports(
    val simpleTypeImports: Map<String, FqName>,
    val staticSingleImports: Map<String, FqName>,
    val typeStarImports: List<FqName>,
    val staticStarImports: List<FqName>,
) {
    /** Unified single-import lookup: [simpleTypeImports] first, then [staticSingleImports]. */
    fun getSingleImport(simpleName: String): FqName? =
        simpleTypeImports[simpleName] ?: staticSingleImports[simpleName]

    companion object {
        val EMPTY: JavaImports = JavaImports(emptyMap(), emptyMap(), emptyList(), emptyList())
    }
}

/**
 * Extracts the package name and import declarations (normal, static, error-recovery, fragmented)
 * from a compilation-unit AST.
 */
internal object JavaImportResolver {

    fun extractPackageName(tree: JavaLightTree, root: JavaLightNode): FqName {
        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = packageStmt?.let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        }
        return if (packageName != null) FqName(packageName) else FqName.ROOT
    }

    /**
     * Extracts all import declarations into a [JavaImports] holder, covering the well-formed
     * case plus two parser-recovery shapes (ERROR_ELEMENT inside / outside IMPORT_LIST).
     */
    fun extractImports(tree: JavaLightTree, root: JavaLightNode): JavaImports {
        val simpleTypeImports = mutableMapOf<String, FqName>()
        val staticSingleImports = mutableMapOf<String, FqName>()
        val typeStarImports = mutableListOf<FqName>()
        val staticStarImports = mutableListOf<FqName>()

        val importList = tree.findChildByType(root, JavaSyntaxElementType.IMPORT_LIST)
            ?: tree.findChildByType(root, JavaSyntaxElementType.CLASS)?.let { tree.findChildByType(it, JavaSyntaxElementType.IMPORT_LIST) }

        if (importList != null) {
            extractNormalImports(tree, importList, simpleTypeImports, typeStarImports)
            extractStaticImports(tree, importList, staticSingleImports, staticStarImports)
            extractErrorElementImports(tree, importList, simpleTypeImports, typeStarImports)
        }

        // Fast path: fragmented imports only occur when the parser emits ERROR_ELEMENT children
        // at the root level. For well-formed files (the common case) there are none, so we can
        // skip walking `root.children` entirely.
        if (tree.getChildren(root).any { tree.getType(it) == SyntaxTokenTypes.ERROR_ELEMENT }) {
            // Fragmented recovery never sees a `static` keyword (the parser splits on `import`,
            // not on `import static`), so fragmented entries are always *type* imports.
            extractFragmentedImports(tree, root, simpleTypeImports, typeStarImports)
        }

        return JavaImports(simpleTypeImports, staticSingleImports, typeStarImports, staticStarImports)
    }

    private fun extractNormalImports(
        tree: JavaLightTree,
        importList: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (importNode in tree.getChildrenByType(importList, JavaSyntaxElementType.IMPORT_STATEMENT)) {
            val codeRef = tree.findChildByType(importNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: continue
            val hasStar = tree.getChildren(importNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
            val fqName = tree.getText(codeRef).toString()

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
                // Keep first occurrence: duplicate explicit imports for the same simple name
                // are a compile error in Java. PSI uses first-seen semantics, so we do too.
                val simpleName = fqName.substringAfterLast('.')
                simpleImports.putIfAbsent(simpleName, FqName(fqName))
            }
        }
    }

    /**
     * Handles `IMPORT_STATIC_STATEMENT` in two parser shapes:
     * - Single static import (`import static X.Y;`): KMP parser emits a single
     *   `IMPORT_STATIC_REFERENCE` child with the full FQN. Routed to [staticSingleImports].
     * - Static-on-demand (`import static X.*;`): KMP parser emits a `JAVA_CODE_REFERENCE`
     *   (the outer class's FQN) followed by sibling `DOT`, `ASTERISK`, `SEMICOLON` tokens
     *   — there is no `IMPORT_STATIC_REFERENCE` node in that shape. Routed to
     *   [staticStarImports] with the *outer class* FqName as the value (not a package).
     */
    private fun extractStaticImports(
        tree: JavaLightTree,
        importList: JavaLightNode,
        staticSingleImports: MutableMap<String, FqName>,
        staticStarImports: MutableList<FqName>,
    ) {
        for (importNode in tree.getChildrenByType(importList, JavaSyntaxElementType.IMPORT_STATIC_STATEMENT)) {
            val hasStar = tree.getChildren(importNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
            val refNode = tree.findChildByType(importNode, JavaSyntaxElementType.IMPORT_STATIC_REFERENCE)
                ?: tree.findChildByType(importNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
                ?: continue
            val fqName = tree.getText(refNode).toString()

            if (hasStar) {
                staticStarImports.add(FqName(fqName))
            } else {
                val simpleName = fqName.substringAfterLast('.')
                staticSingleImports.putIfAbsent(simpleName, FqName(fqName))
            }
        }
    }

    /**
     * Recovers imports emitted as ERROR_ELEMENT inside IMPORT_LIST — happens when the import
     * starts with a reserved word (e.g. `import kotlin.X;`); IDENTIFIER/DOT children survive.
     * Error-recovered imports are always treated as *type* imports (the recovery path does not
     * preserve a `static` keyword distinction).
     */
    private fun extractErrorElementImports(
        tree: JavaLightTree,
        importList: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        for (errorNode in tree.getChildrenByType(importList, SyntaxTokenTypes.ERROR_ELEMENT)) {
            if (tree.findChildByType(errorNode, JavaSyntaxTokenType.IMPORT_KEYWORD) == null) continue

            // Reconstruct the import from IDENTIFIER and DOT children
            val identifiers = mutableListOf<String>()
            for (child in tree.getChildren(errorNode)) {
                if (tree.getType(child) == JavaSyntaxTokenType.IDENTIFIER) {
                    identifiers.add(tree.getText(child).toString())
                }
            }
            if (identifiers.isEmpty()) continue

            val hasStar = tree.getChildren(errorNode).any { tree.getType(it) == JavaSyntaxTokenType.ASTERISK }
            val fqName = identifiers.joinToString(".")

            if (hasStar) {
                starImports.add(FqName(fqName))
            } else {
                simpleImports.putIfAbsent(identifiers.last(), FqName(fqName))
            }
        }
    }

    /**
     * Recovers imports the parser has split across sibling nodes of the compilation-unit root.
     * Two shapes: `ERROR_ELEMENT(import) + TYPE(JAVA_CODE_REFERENCE)` (simple), or additionally
     * followed by `ERROR_ELEMENT(*;)` (star). MODIFIER_LIST / whitespace between nodes are skipped.
     * Always treated as *type* imports — see [extractErrorElementImports].
     */
    private fun extractFragmentedImports(
        tree: JavaLightTree,
        root: JavaLightNode,
        simpleImports: MutableMap<String, FqName>,
        starImports: MutableList<FqName>,
    ) {
        val allChildren = tree.getChildren(root)
        var i = 0
        while (i < allChildren.size) {
            val node = allChildren[i]
            val nodeType = tree.getType(node)

            val isImportError = nodeType == SyntaxTokenTypes.ERROR_ELEMENT &&
                    (tree.findChildByType(node, JavaSyntaxTokenType.IMPORT_KEYWORD) != null ||
                            tree.getText(node).toString().trim() == "import")

            if (isImportError) {
                val target = findTypeNodeAndStar(tree, allChildren, i)
                if (target != null) {
                    val ref = tree.findChildByType(target.typeNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: target.typeNode
                    var fqName = tree.getText(ref).toString().trim()
                    if (fqName.endsWith('.')) {
                        fqName = fqName.dropLast(1)
                    }

                    if (fqName.isNotEmpty()) {
                        if (target.hasStar) {
                            // Single-segment star imports (`import lombok.*;`) are valid Java;
                            // the dot guard would have wrongly skipped them.
                            starImports.add(FqName(fqName))
                        } else if (fqName.contains('.')) {
                            // Simple non-star imports must be fully qualified; single-segment
                            // forms (`import Foo;`) are illegal and not recovered here.
                            val simpleName = fqName.substringAfterLast('.')
                            simpleImports.putIfAbsent(simpleName, FqName(fqName))
                        }
                    }
                }
            }
            i++
        }
    }

    private data class FragmentedImportTarget(val typeNode: JavaLightNode, val hasStar: Boolean)

    /**
     * Starting at the `import`-shaped ERROR_ELEMENT at `allChildren[startIdx]`, finds the TYPE /
     * JAVA_CODE_REFERENCE sibling carrying the FQN and probes one more step for a trailing `*`.
     * Returns `null` if a CLASS boundary is hit first (unrelated parser error).
     */
    private fun findTypeNodeAndStar(
        tree: JavaLightTree,
        allChildren: List<JavaLightNode>,
        startIdx: Int,
    ): FragmentedImportTarget? {
        var hasStar = false
        for (j in (startIdx + 1) until allChildren.size) {
            val sibling = allChildren[j]
            val siblingType = tree.getType(sibling)
            if (siblingType == JavaSyntaxElementType.MODIFIER_LIST) continue
            if (siblingType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(sibling).toString().isBlank()) continue

            if (siblingType == JavaSyntaxElementType.TYPE || siblingType == JavaSyntaxElementType.JAVA_CODE_REFERENCE) {
                for (k in (j + 1) until allChildren.size) {
                    val nextSib = allChildren[k]
                    val nextType = tree.getType(nextSib)
                    if (nextType == JavaSyntaxElementType.MODIFIER_LIST) continue
                    if (nextType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(nextSib).toString().isBlank()) continue
                    if (nextType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(nextSib).toString().contains("*")) {
                        hasStar = true
                        break
                    }
                    if (nextType == JavaSyntaxElementType.CLASS) break
                }
                return FragmentedImportTarget(sibling, hasStar)
            }
            if (siblingType == SyntaxTokenTypes.ERROR_ELEMENT && tree.getText(sibling).toString().contains("*")) {
                hasStar = true
            }
            if (siblingType == JavaSyntaxElementType.CLASS) break
        }
        return null
    }
}
