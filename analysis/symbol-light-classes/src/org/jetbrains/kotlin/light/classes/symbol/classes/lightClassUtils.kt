/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*


context(_: KaSession)
internal fun KaClassSymbol.shouldNotBeVisibleAsLightClass(): Boolean {
    val containingModule = containingModule
    if ((containingModule as? KaDanglingFileModule)?.isCodeFragment == true) {
        // Avoid building light classes for code fragments
        return true
    }

    // Avoid building light classes for decompiled built-ins
    if ((containingModule is KaLibraryModule || containingModule is KaBuiltinsModule) && isBuiltinClass()) {
        return true
    }

    if (isExpect) {
        return true
    }

    val classOrObjectPsi = sourcePsiSafe<KtClassOrObject>()
    if (isLocal && classOrObjectPsi != null) {
        if ((containingFile?.psi as? KtFile)?.virtualFile == null) return true
        if (hasParseErrorsAround(classOrObjectPsi) || PsiUtilCore.hasErrorElementChild(classOrObjectPsi)) return true
        if (classDeclaredInUnexpectedPosition(classOrObjectPsi)) return true
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

private fun KaClassSymbol.isBuiltinClass(): Boolean {
    val classId = classId ?: return false
    return classId.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME) &&
            (JavaToKotlinClassMap.isMappedKotlinClass(classId) || classId.isArrayType())
}

/**
 * [JavaToKotlinClassMap] doesn't contain any Kotlin -> Java mappings for array types.
 * That's because Kotlin array types are mapped into regular array structures in Java.
 * Hence, those have to be checked manually in [isBuiltinClass].
 */
private fun ClassId.isArrayType(): Boolean {
    return this == StandardClassIds.Array ||
            this in StandardClassIds.elementTypeByPrimitiveArrayType ||
            this in StandardClassIds.elementTypeByUnsignedArrayType
}
