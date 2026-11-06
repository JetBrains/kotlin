/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.modules

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaAnnotationOverAst
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.LeanJavaClassFinder
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

/**
 * Reads a `module-info.java` into a [JavaModuleInfo]: the module name, `requires [transitive]`, `exports … to`
 * and the annotations of the module declaration. `opens`, `uses` and `provides` do not take part in resolution.
 *
 * The module annotations are resolved lazily, like those of a `module-info.class`, through [classesByClassId]:
 * at classpath setup there is no FIR session yet, so the classpath is the only place the annotation classes
 * (`org.jspecify.annotations.NullMarked` and friends) can come from.
 *
 * Returns `null` when the text declares no module.
 */
fun readJavaModuleInfoSource(text: CharSequence, classesByClassId: ClassIdToJavaClass): JavaModuleInfo? {
    val tree = parseJavaToLightTree(text, 0)
    val module = tree.findChildByType(tree.getRoot(), JavaSyntaxElementType.MODULE) ?: return null
    val moduleName = tree.findChildByType(module, JavaSyntaxElementType.MODULE_REFERENCE)?.let { tree.nameOf(it) } ?: return null

    val requires = tree.getChildrenByType(module, JavaSyntaxElementType.REQUIRES_STATEMENT).mapNotNull { statement ->
        val requiredModule = tree.findChildByType(statement, JavaSyntaxElementType.MODULE_REFERENCE) ?: return@mapNotNull null
        val modifiers = tree.findChildByType(statement, JavaSyntaxElementType.MODIFIER_LIST)
        val isTransitive = modifiers != null && tree.hasChildOfType(modifiers, JavaSyntaxTokenType.TRANSITIVE_KEYWORD)
        JavaModuleInfo.Requires(tree.nameOf(requiredModule), isTransitive)
    }

    val exports = tree.getChildrenByType(module, JavaSyntaxElementType.EXPORTS_STATEMENT).mapNotNull { statement ->
        val packageReference = tree.findChildByType(statement, JavaSyntaxElementType.JAVA_CODE_REFERENCE) ?: return@mapNotNull null
        val toModules = tree.getChildrenByType(statement, JavaSyntaxElementType.MODULE_REFERENCE).map { tree.nameOf(it) }
        JavaModuleInfo.Exports(FqName(tree.nameOf(packageReference)), toModules)
    }

    val annotations = tree.findChildByType(module, JavaSyntaxElementType.MODIFIER_LIST)
        ?.let { modifiers -> tree.getChildrenByType(modifiers, JavaSyntaxElementType.ANNOTATION) }
        .orEmpty()
        .takeIf { it.isNotEmpty() }
        ?.let { annotationNodes ->
            val resolutionContext = JavaResolutionContext.create(tree, session = null, classFinder = ClasspathClassFinder(classesByClassId))
            annotationNodes.map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }
        .orEmpty()

    return JavaModuleInfo(moduleName, requires, exports, annotations)
}

/** The text of a dotted name, without the whitespace its segments may be separated by. */
private fun LightSyntaxTree.nameOf(reference: LightNode): String = getText(reference).toString().filterNot(Char::isWhitespace)

/** Every class of the classpath is "in the index": the lookup itself decides whether a class exists. */
private class ClasspathClassFinder(private val classesByClassId: ClassIdToJavaClass) : LeanJavaClassFinder {
    override fun isClassInIndex(classId: ClassId): Boolean = classesByClassId(classId) != null

    override fun findClass(request: JavaClassFinder.Request): JavaClass? = classesByClassId(request.classId)
}
