/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.generateResolvedAccessExpression
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.lightTree.fir.TypeConstraint
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateExpressionElementType
import org.jetbrains.kotlin.types.Variance

private val expressionSet = listOf(
    REFERENCE_EXPRESSION,
    DOT_QUALIFIED_EXPRESSION,
    LAMBDA_EXPRESSION,
    FUN
)

val qualifiedAccessTokens = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)

fun String?.nameAsSafeName(defaultName: String = ""): Name {
    return when {
        this != null -> Name.identifier(this.replace("`", ""))
        defaultName.isNotEmpty() -> Name.identifier(defaultName)
        else -> SpecialNames.NO_NAME_PROVIDED
    }
}

fun String.getOperationSymbol(): IElementType {
    KotlinExpressionParsing.ALL_OPERATIONS.types.forEach {
        if (it is KtSingleValueToken && it.value == this) return it
    }
    if (this == "as?") return KtTokens.AS_SAFE
    return KtTokens.IDENTIFIER
}

fun LighterASTNode.getAsStringWithoutBacktick(): String {
    return this.toString().replace("`", "")
}

fun LighterASTNode.isExpression(): Boolean {
    return when (this.tokenType) {
        is KtNodeType,
        is KtConstantExpressionElementType,
        is KtStringTemplateExpressionElementType,
        in expressionSet -> true
        else -> false
    }
}

fun FirTypeParameterContainer.joinTypeParameters(typeConstraints: List<TypeConstraint>) {
    typeConstraints.forEach { typeConstraint ->
        this.typeParameters.forEach { typeParameter ->
            if (typeConstraint.identifier == typeParameter.name.identifier) {
                (typeParameter as FirTypeParameterImpl).bounds += typeConstraint.firTypeRef
                typeParameter.annotations += typeConstraint.firTypeRef.annotations
                typeParameter.annotations += typeConstraint.annotations
            }
        }
    }
}

fun <T : FirCallWithArgumentList> T.extractArgumentsFrom(container: List<FirExpression>, stubMode: Boolean): T {
    if (!stubMode || this is FirAnnotationCall) {
        this.arguments += container
    }
    return this
}

inline fun isClassLocal(classNode: LighterASTNode, getParent: LighterASTNode.() -> LighterASTNode?): Boolean {
    var currentNode: LighterASTNode? = classNode
    while (currentNode != null) {
        val tokenType = currentNode.tokenType
        val parent = currentNode.getParent()
        if (tokenType == PROPERTY || tokenType == FUN) {
            val grandParent = parent?.getParent()
            when {
                parent?.tokenType == KT_FILE -> return true
                parent?.tokenType == CLASS_BODY && !(grandParent?.tokenType == OBJECT_DECLARATION && grandParent?.getParent()?.tokenType == OBJECT_LITERAL) -> return true
                parent?.tokenType == BLOCK && grandParent?.tokenType == SCRIPT -> return true
            }
        }
        if (tokenType == BLOCK) {
            return true
        }
        currentNode = parent
    }
    return false
}

fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: DestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean
): FirExpression {
    return FirBlockImpl(session, null).apply {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            statements += FirVariableImpl(
                session, null, entry.name,
                entry.returnTypeRef, isVar,
                FirComponentCallImpl(session, null, index + 1, generateResolvedAccessExpression(session, null, container)),
                FirVariableSymbol(entry.name) // TODO?
            ).apply {
                annotations += entry.annotations
                symbol.bind(this)
            }
        }
    }
}
