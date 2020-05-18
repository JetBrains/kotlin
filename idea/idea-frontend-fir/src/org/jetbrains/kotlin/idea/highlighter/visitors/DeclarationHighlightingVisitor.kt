/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.highlighter.textAttributesKeyForPropertyDeclaration
import org.jetbrains.kotlin.idea.highlighter.textAttributesKeyForTypeDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors as Colors

internal class DeclarationHighlightingVisitor(
    analysisSession: FrontendAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitNamedFunction(function: KtNamedFunction) {
        highlightNamedDeclaration(function, Colors.FUNCTION_DECLARATION)
        super.visitNamedFunction(function)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        highlightNamedDeclaration(typeAlias, Colors.TYPE_ALIAS)
        super.visitTypeAlias(typeAlias)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        textAttributesKeyForTypeDeclaration(classOrObject)?.let { attributes -> highlightNamedDeclaration(classOrObject, attributes) }
        super.visitClassOrObject(classOrObject)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        highlightNamedDeclaration(parameter, Colors.TYPE_PARAMETER)
        super.visitTypeParameter(parameter)
    }

    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
        if (declaration is KtValVarKeywordOwner) {
            textAttributesKeyForPropertyDeclaration(declaration)?.let { attributes ->
                highlightNamedDeclaration(declaration, attributes)
            }
            if (declaration.valOrVarKeyword?.elementType == KtTokens.VAR_KEYWORD) {
                highlightNamedDeclaration(declaration, Colors.MUTABLE_VARIABLE)
            }
        }
        super.visitNamedDeclaration(declaration)
    }


    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeElement = calleeExpression.typeReference?.typeElement
        if (typeElement is KtUserType) {
            typeElement.referenceExpression?.let { highlightName(it, Colors.CONSTRUCTOR_CALL) }
        }
        super.visitSuperTypeCallEntry(call)
    }
}