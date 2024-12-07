/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.CommonClassNames
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

fun KtClassOrObject.defaultJavaAncestorQualifiedName(): String? {
    if (this !is KtClass) return CommonClassNames.JAVA_LANG_OBJECT

    return when {
        isAnnotation() -> CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION
        isEnum() -> CommonClassNames.JAVA_LANG_ENUM
        isInterface() -> CommonClassNames.JAVA_LANG_OBJECT // see com.intellij.psi.impl.PsiClassImplUtil.getSuperClass
        else -> CommonClassNames.JAVA_LANG_OBJECT
    }
}

fun KtClassOrObject.shouldNotBeVisibleAsLightClass(): Boolean {
    val containingFile = containingFile
    if (containingFile is KtCodeFragment) {
        // Avoid building light classes for code fragments
        return true
    }

    // Avoid building light classes for decompiled built-ins
    if ((containingFile as? KtFile)?.isCompiled == true &&
        containingFile.virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
    ) {
        return true
    }

    if (parentsWithSelf.filterIsInstance<KtClassOrObject>().any { it.hasExpectModifier() }) {
        return true
    }

    if (isLocal) {
        if (containingFile.virtualFile == null) return true
        if (hasParseErrorsAround(this) || PsiUtilCore.hasErrorElementChild(this)) return true
        if (classDeclaredInUnexpectedPosition(this)) return true
    }

    if (isEnumEntryWithoutBody(this)) {
        return true
    }

    return false
}

/**
 * If class is declared in some strange context (for example, in expression like `10 < class A`),
 * we don't want to try to build a light class for it.
 *
 * The expression itself is incorrect and won't compile, but the parser is able the parse the class nonetheless.
 *
 * This does not concern objects, since object literals are expressions and can be used almost anywhere.
 */
private fun classDeclaredInUnexpectedPosition(classOrObject: KtClassOrObject): Boolean {
    if (classOrObject is KtObjectDeclaration) return false

    val classParent = classOrObject.parent

    return classParent !is KtBlockExpression &&
            classParent !is KtDeclarationContainer
}

private fun isEnumEntryWithoutBody(classOrObject: KtClassOrObject): Boolean {
    if (classOrObject !is KtEnumEntry) {
        return false
    }
    return classOrObject.getBody()?.declarations?.isEmpty() ?: true
}

private fun hasParseErrorsAround(psi: PsiElement): Boolean {
    val node = psi.node ?: return false

    TreeUtil.nextLeaf(node)?.let { nextLeaf ->
        if (nextLeaf.elementType == TokenType.ERROR_ELEMENT || nextLeaf.treePrev?.elementType == TokenType.ERROR_ELEMENT) {
            return true
        }
    }

    TreeUtil.prevLeaf(node)?.let { prevLeaf ->
        if (prevLeaf.elementType == TokenType.ERROR_ELEMENT || prevLeaf.treeNext?.elementType == TokenType.ERROR_ELEMENT) {
            return true
        }
    }

    return false
}

fun getOutermostClassOrObject(classOrObject: KtClassOrObject): KtClassOrObject {
    return KtPsiUtil.getOutermostClassOrObject(classOrObject)
        ?: throw IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.text)
}
