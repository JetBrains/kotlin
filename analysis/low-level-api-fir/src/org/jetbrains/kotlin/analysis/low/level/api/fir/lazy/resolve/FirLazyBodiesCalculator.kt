/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Deprecated
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DeprecatedSinceKotlin
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRecord
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.WasExperimental
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.SinceKotlin

internal object FirLazyBodiesCalculator {
    fun calculateLazyBodiesInside(designation: FirDesignation) {
        calculateAnnotations(designation.target)
        designation.target.transform<FirElement, PersistentList<FirRegularClass>>(
            FirLazyBodiesCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    fun calculateLazyBodies(firFile: FirFile) {
        calculateAnnotations(firFile)
        firFile.transform<FirElement, PersistentList<FirRegularClass>>(FirLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateAnnotations(firElement: FirElementWithResolvePhase) {
        calculateAnnotations(firElement, firElement.moduleData.session)
    }

    fun calculateAnnotations(firElement: FirElement, session: FirSession) {
        firElement.transform<FirElement, FirLazyAnnotationTransformerData>(
            FirLazyAnnotationTransformer,
            FirLazyAnnotationTransformerData(session)
        )
    }

    fun calculateCompilerAnnotations(firElement: FirElementWithResolvePhase) {
        firElement.transform<FirElement, FirLazyAnnotationTransformerData>(
            FirLazyAnnotationTransformer,
            FirLazyAnnotationTransformerData(firElement.moduleData.session, FirLazyAnnotationTransformerScope.COMPILER_ONLY)
        )
    }

    private fun replaceValueParameterDefaultValues(valueParameters: List<FirValueParameter>, newValueParameters: List<FirValueParameter>) {
        require(valueParameters.size == newValueParameters.size)
        for ((valueParameter, newValueParameter) in valueParameters.zip(newValueParameters)) {
            if (newValueParameter.defaultValue != null) {
                valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
            }
        }
    }

    fun calculateLazyArgumentsForAnnotation(annotationCall: FirAnnotationCall, session: FirSession) {
        require(needCalculatingAnnotationCall(annotationCall))
        val builder = RawFirBuilder(session, baseScopeProvider = session.kotlinScopeProvider)
        val newAnnotationCall = builder.buildAnnotationCall(annotationCall.psi as KtAnnotationEntry)
        annotationCall.replaceArgumentList(newAnnotationCall.argumentList)
    }

    fun calculateLazyBodiesForFunction(designation: FirDesignation) {
        val simpleFunction = designation.target as FirSimpleFunction
        require(needCalculatingLazyBodyForFunction(simpleFunction))
        val newFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = simpleFunction.moduleData.session,
            scopeProvider = simpleFunction.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = simpleFunction.psi as KtNamedFunction,
        ) as FirSimpleFunction
        simpleFunction.apply {
            replaceBody(newFunction.body)
            replaceContractDescription(newFunction.contractDescription)
            replaceValueParameterDefaultValues(valueParameters, newFunction.valueParameters)
        }
    }

    fun calculateLazyBodyForConstructor(designation: FirDesignation) {
        val constructor = designation.target as FirConstructor
        require(needCalculatingLazyBodyForConstructor(constructor))

        val newConstructor = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = constructor.moduleData.session,
            scopeProvider = constructor.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = constructor.psi as KtDeclaration,
        ) as FirConstructor

        constructor.apply {
            replaceBody(newConstructor.body)
            replaceContractDescription(newConstructor.contractDescription)
            replaceDelegatedConstructor(newConstructor.delegatedConstructor)
            replaceValueParameterDefaultValues(valueParameters, newConstructor.valueParameters)
        }
    }

    fun calculateLazyBodyForProperty(designation: FirDesignation) {
        val firProperty = designation.target as FirProperty
        if (!needCalculatingLazyBodyForProperty(firProperty)) return

        val newProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = firProperty.moduleData.session,
            scopeProvider = firProperty.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = firProperty.psi as KtProperty
        ) as FirProperty

        firProperty.getter?.takeIf { it.body is FirLazyBlock }?.let { getter ->
            val newGetter = newProperty.getter!!
            getter.replaceBody(newGetter.body)
            getter.replaceContractDescription(newGetter.contractDescription)
        }

        firProperty.setter?.takeIf { it.body is FirLazyBlock }?.let { setter ->
            val newSetter = newProperty.setter!!
            setter.replaceBody(newSetter.body)
            setter.replaceContractDescription(newSetter.contractDescription)
        }

        if (firProperty.initializer is FirLazyExpression) {
            firProperty.replaceInitializer(newProperty.initializer)
        }

        firProperty.getExplicitBackingField()?.takeIf { it.initializer is FirLazyExpression }?.let { backingField ->
            val newInitializer = newProperty.getExplicitBackingField()?.initializer
            backingField.replaceInitializer(newInitializer)
        }

        val delegate = firProperty.delegate as? FirWrappedDelegateExpression
        val delegateExpression = delegate?.expression
        if (delegateExpression is FirLazyExpression) {
            val newDelegate = newProperty.delegate as? FirWrappedDelegateExpression
            check(newDelegate != null) { "Invalid replacement delegate" }
            delegate.replaceExpression(newDelegate.expression)
            delegate.replaceDelegateProvider(newDelegate.delegateProvider)
        }
    }

    fun calculateLazyInitializerForEnumEntry(designation: FirDesignation) {
        val enumEntry = designation.target as FirEnumEntry
        require(enumEntry.initializer is FirLazyExpression)
        val newEntry = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = enumEntry.moduleData.session,
            scopeProvider = enumEntry.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = enumEntry.psi as KtEnumEntry,
        ) as FirEnumEntry
        enumEntry.apply {
            replaceInitializer(newEntry.initializer)
        }
    }

    fun calculateLazyBodyForAnonymousInitializer(designation: FirDesignation) {
        val initializer = designation.target as FirAnonymousInitializer
        require(initializer.body is FirLazyBlock)
        val newInitializer = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = initializer.moduleData.session,
            scopeProvider = initializer.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = initializer.psi as KtAnonymousInitializer,
        ) as FirAnonymousInitializer
        initializer.apply {
            replaceBody(newInitializer.body)
        }
    }

    fun needCalculatingLazyBodyForConstructor(firConstructor: FirConstructor): Boolean =
        needCalculatingLazyBodyForFunction(firConstructor) || firConstructor.delegatedConstructor is FirLazyDelegatedConstructorCall

    fun needCalculatingLazyBodyForFunction(firFunction: FirFunction): Boolean =
        firFunction.body is FirLazyBlock || firFunction.valueParameters.any { it.defaultValue is FirLazyExpression }

    fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
        firProperty.getter?.let { needCalculatingLazyBodyForFunction(it) } == true
                || firProperty.setter?.let { needCalculatingLazyBodyForFunction(it) } == true
                || firProperty.initializer is FirLazyExpression
                || (firProperty.delegate as? FirWrappedDelegateExpression)?.expression is FirLazyExpression
                || firProperty.getExplicitBackingField()?.initializer is FirLazyExpression

    fun needCalculatingAnnotationCall(firAnnotationCall: FirAnnotationCall): Boolean =
        firAnnotationCall.argumentList.arguments.any { it is FirLazyExpression }
}

private enum class FirLazyAnnotationTransformerScope {
    ALL_ANNOTATIONS,
    COMPILER_ONLY;
}

private data class FirLazyAnnotationTransformerData(
    val session: FirSession,
    val compilerAnnotationsOnly: FirLazyAnnotationTransformerScope = FirLazyAnnotationTransformerScope.ALL_ANNOTATIONS
)

private object FirLazyAnnotationTransformer : FirTransformer<FirLazyAnnotationTransformerData>() {
    private val COMPILER_ANNOTATION_NAMES: Set<Name> = setOf(
        Deprecated,
        DeprecatedSinceKotlin,
        WasExperimental,
        JvmRecord,
        SinceKotlin,
    ).mapTo(mutableSetOf()) { it.shortClassName }

    private fun canBeCompilerAnnotation(annotationCall: FirAnnotationCall, session: FirSession): Boolean {
        val annotationTypeRef = annotationCall.annotationTypeRef
        if (annotationTypeRef !is FirUserTypeRef) return false
        if (session.registeredPluginAnnotations.annotations.isNotEmpty()) return true
        val name = annotationTypeRef.qualifier.last().name
        return name in COMPILER_ANNOTATION_NAMES
    }

    override fun <E : FirElement> transformElement(element: E, data: FirLazyAnnotationTransformerData): E {
        element.transformChildren(this, data)
        return element
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: FirLazyAnnotationTransformerData): FirStatement {
        val shouldCalculate = data.compilerAnnotationsOnly == FirLazyAnnotationTransformerScope.ALL_ANNOTATIONS ||
                canBeCompilerAnnotation(annotationCall, data.session)
        if (shouldCalculate && FirLazyBodiesCalculator.needCalculatingAnnotationCall(annotationCall)) {
            FirLazyBodiesCalculator.calculateLazyArgumentsForAnnotation(annotationCall, data.session)
        }
        super.transformAnnotationCall(annotationCall, data)
        return annotationCall
    }

    override fun transformErrorAnnotationCall(
        errorAnnotationCall: FirErrorAnnotationCall,
        data: FirLazyAnnotationTransformerData
    ): FirStatement {
        transformAnnotationCall(errorAnnotationCall, data)
        return errorAnnotationCall
    }

    override fun transformExpression(expression: FirExpression, data: FirLazyAnnotationTransformerData): FirStatement {
        if (expression is FirLazyExpression) {
            return expression
        }
        return super.transformExpression(expression, data)
    }

    override fun transformBlock(block: FirBlock, data: FirLazyAnnotationTransformerData): FirStatement {
        if (block is FirLazyBlock) {
            return block
        }
        return super.transformBlock(block, data)
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: FirLazyAnnotationTransformerData
    ): FirStatement {
        if (delegatedConstructorCall is FirLazyDelegatedConstructorCall) {
            return delegatedConstructorCall
        }
        return super.transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }
}

private object FirLazyBodiesCalculatorTransformer : FirTransformer<PersistentList<FirRegularClass>>() {

    override fun transformFile(file: FirFile, data: PersistentList<FirRegularClass>): FirFile {
        file.declarations.forEach {
            it.transformSingle(this, data)
        }
        return file
    }

    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirRegularClass>): E {
        if (element is FirRegularClass) {
            val newList = data.add(element)
            element.declarations.forEach {
                it.transformSingle(this, newList)
            }
            element.transformChildren(this, newList)
        }
        return element
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirRegularClass>
    ): FirSimpleFunction {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForFunction(simpleFunction)) {
            val designation = FirDesignation(data, simpleFunction)
            FirLazyBodiesCalculator.calculateLazyBodiesForFunction(designation)
        }
        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirRegularClass>
    ): FirConstructor {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForConstructor(constructor)) {
            val designation = FirDesignation(data, constructor)
            FirLazyBodiesCalculator.calculateLazyBodyForConstructor(designation)
        }
        return constructor
    }

    override fun transformProperty(property: FirProperty, data: PersistentList<FirRegularClass>): FirProperty {
        if (FirLazyBodiesCalculator.needCalculatingLazyBodyForProperty(property)) {
            val designation = FirDesignation(data, property)
            FirLazyBodiesCalculator.calculateLazyBodyForProperty(designation)
        }
        return property
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: PersistentList<FirRegularClass>): FirStatement {
        return propertyAccessor.also { transformProperty(it.propertySymbol.fir, data) }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: PersistentList<FirRegularClass>): FirStatement {
        if (enumEntry.initializer is FirLazyExpression) {
            val designation = FirDesignation(data, enumEntry)
            FirLazyBodiesCalculator.calculateLazyInitializerForEnumEntry(designation)
        }
        return enumEntry
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer, data: PersistentList<FirRegularClass>
    ): FirAnonymousInitializer {
        if (anonymousInitializer.body is FirLazyBlock) {
            val designation = FirDesignation(data, anonymousInitializer)
            FirLazyBodiesCalculator.calculateLazyBodyForAnonymousInitializer(designation)
        }
        return anonymousInitializer
    }
}
