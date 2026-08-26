/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.analysis.NodeTypeAnalyzer
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.lightTree.fir.ClassWrapper
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef

interface TreeDeclarationConverter<Node : Any> {
    fun convertObjectLiteral(node: Node): FirAnonymousObjectExpression
    fun convertDestructingDeclaration(node: Node): DestructuringDeclaration
    fun convertFunctionDeclaration(node: Node): FirStatement
    fun convertPropertyDeclaration(node: Node, classWrapper: ClassWrapper<Node>? = null): FirProperty
    fun convertClass(node: Node): FirRegularClass
    fun convertTypeAlias(node: Node): FirTypeAlias
    fun convertBlock(node: Node?, convertOnlyFirstStatement: Boolean = false): FirBlock
    fun convertBlockExpression(node: Node, convertOnlyFirstStatement: Boolean = false): FirBlock
    fun convertAnnotationTo(node: Node, list: MutableList<in FirAnnotationCall>)
    fun convertTypeArguments(typeArguments: Node, allowedUnderscoredTypeArgument: Boolean): List<FirTypeProjection>
    fun convertType(type: Node): FirTypeRef

    fun convertAnnotationEntry(
        node: Node,
        defaultAnnotationUseSiteTarget: AnnotationUseSiteTarget? = null,
        diagnostic: ConeDiagnostic? = null,
    ): FirAnnotationCall

    fun convertValueParameters(
        valueParameters: Node,
        functionSymbol: FirFunctionSymbol<*>,
        valueParameterDeclaration: NodeTypeAnalyzer.ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList(),
    ): List<ValueParameter<Node>>

    fun convertValueParameter(
        valueParameter: Node,
        containingDeclarationSymbol: FirBasedSymbol<*>?,
        valueParameterDeclaration: NodeTypeAnalyzer.ValueParameterDeclaration,
        additionalAnnotations: List<FirAnnotation> = emptyList()
    ): ValueParameter<Node>

    fun convertBlockExpressionWithoutBuilding(
        block: Node,
        kind: KtFakeSourceElementKind? = null,
        convertOnlyFirstStatement: Boolean = false
    ): FirBlockBuilder
}
