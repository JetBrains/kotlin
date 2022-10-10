/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignation
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.psi.*

internal object FirLazyBodiesCalculator {
    fun calculateLazyBodiesInside(designation: FirDeclarationDesignation, contractableOnly: Boolean = false) {
        designation.declaration.transform<FirElement, PersistentList<FirDeclaration>>(
            FirLazyBodiesCalculatorTransformer(contractableOnly),
            designation.toSequence(includeTarget = false).toList().toPersistentList()
        )
    }

    fun calculateLazyBodies(firFile: FirFile) {
        firFile.transform<FirElement, PersistentList<FirDeclaration>>(FirLazyBodiesCalculatorTransformer(), persistentListOf())
    }

    private fun replaceAnnotations(annotations: List<FirAnnotation>, newAnnotations: List<FirAnnotation>) {
        require(annotations.size == newAnnotations.size)
        for ((annotation, newAnnotation) in annotations.zip(newAnnotations)) {
            if (annotation is FirAnnotationCall && newAnnotation is FirAnnotationCall) {
                annotation.replaceArgumentList(newAnnotation.argumentList)
            }
        }
    }

    private fun replaceValueParameterDefaultValues(valueParameters: List<FirValueParameter>, newValueParameters: List<FirValueParameter>) {
        require(valueParameters.size == newValueParameters.size)
        for ((valueParameter, newValueParameter) in valueParameters.zip(newValueParameters)) {
            if (newValueParameter.defaultValue != null) {
                valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
            }
        }
    }

    fun calculateLazyBodiesForFunction(designation: FirDeclarationDesignation) {
        val simpleFunction = designation.declaration as FirSimpleFunction
        if (!needCalculatingForFunction(simpleFunction)) return
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
            replaceAnnotations(annotations, newFunction.annotations)
        }
    }

    fun calculateLazyBodyForAnonymousInitializer(designation: FirDeclarationDesignation) {
        val initializer = designation.declaration as FirAnonymousInitializer
        if (initializer.body !is FirLazyBlock) return
        val newFunction = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = initializer.moduleData.session,
            scopeProvider = initializer.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = initializer.psi as KtAnonymousInitializer,
        ) as FirAnonymousInitializer
        initializer.apply {
            replaceBody(newFunction.body)
            replaceAnnotations(annotations, newFunction.annotations)
        }
    }

    fun calculateLazyInitializerForEnumEntry(designation: FirDeclarationDesignation) {
        val enumEntry = designation.declaration as FirEnumEntry
        if (enumEntry.initializer !is FirLazyExpression) return
        val newEntry = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = enumEntry.moduleData.session,
            scopeProvider = enumEntry.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = enumEntry.psi as KtEnumEntry,
        ) as FirEnumEntry
        enumEntry.apply {
            replaceInitializer(newEntry.initializer)
            replaceAnnotations(annotations, newEntry.annotations)
        }
    }

    fun calculateLazyBodyForPrimaryConstructor(designation: FirDeclarationDesignation) {
        val primaryConstructor = designation.declaration as FirPrimaryConstructor
        require(primaryConstructor.isPrimary)
        if (!needCalculatingForConstructor(primaryConstructor)) return

        check(designation.declaration.psi !is KtClass)

        val newPrimaryConstructor = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = primaryConstructor.moduleData.session,
            scopeProvider = primaryConstructor.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = primaryConstructor.psi as KtDeclaration,
        ) as FirPrimaryConstructor

        primaryConstructor.apply {
            replaceValueParameterDefaultValues(valueParameters, newPrimaryConstructor.valueParameters)
            val typeRef = delegatedConstructor?.constructedTypeRef
            replaceDelegatedConstructor(newPrimaryConstructor.delegatedConstructor)
            delegatedConstructor?.replaceConstructedTypeRef(typeRef!!)
            replaceAnnotations(annotations, newPrimaryConstructor.annotations)
        }
    }

    fun calculateLazyBodyForSecondaryConstructor(designation: FirDeclarationDesignation) {
        val secondaryConstructor = designation.declaration as FirConstructor
        require(!secondaryConstructor.isPrimary)
        if (!needCalculatingForConstructor(secondaryConstructor)) return

        val newConstructor = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = secondaryConstructor.moduleData.session,
            scopeProvider = secondaryConstructor.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = secondaryConstructor.psi as KtSecondaryConstructor,
        ) as FirConstructor

        secondaryConstructor.apply {
            replaceBody(newConstructor.body)
            replaceValueParameterDefaultValues(valueParameters, newConstructor.valueParameters)
            val typeRef = delegatedConstructor?.constructedTypeRef
            replaceDelegatedConstructor(newConstructor.delegatedConstructor)
            delegatedConstructor?.replaceConstructedTypeRef(typeRef!!)
            replaceAnnotations(annotations, newConstructor.annotations)
        }
    }

    fun calculateLazyBodyForProperty(designation: FirDeclarationDesignation) {
        val firProperty = designation.declaration as FirProperty
        if (!needCalculatingLazyBodyForProperty(firProperty)) return

        val newProperty = RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
            session = firProperty.moduleData.session,
            scopeProvider = firProperty.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = firProperty.psi as KtProperty
        ) as FirProperty

        firProperty.apply {
            replaceAnnotations(annotations, newProperty.annotations)
        }

        firProperty.getter?.takeIf { it.body is FirLazyBlock || it.annotations.isNotEmpty() }?.let { getter ->
            val newGetter = newProperty.getter!!
            getter.replaceBody(newGetter.body)
            getter.replaceContractDescription(newGetter.contractDescription)
            replaceAnnotations(getter.annotations, newGetter.annotations)
        }

        firProperty.setter?.takeIf { it.body is FirLazyBlock || it.annotations.isNotEmpty() }?.let { setter ->
            val newSetter = newProperty.setter!!
            setter.replaceBody(newSetter.body)
            setter.replaceContractDescription(newSetter.contractDescription)
            replaceAnnotations(setter.annotations, newSetter.annotations)
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

            val delegateProviderCall = delegate.delegateProvider as? FirFunctionCall
            val delegateProviderExplicitReceiver = delegateProviderCall?.explicitReceiver
            if (delegateProviderExplicitReceiver is FirLazyExpression) {
                val newDelegateProviderExplicitReceiver = (newDelegate.delegateProvider as? FirFunctionCall)?.explicitReceiver
                check(newDelegateProviderExplicitReceiver != null) { "Invalid replacement expression" }
                delegateProviderCall.replaceExplicitReceiver(newDelegateProviderExplicitReceiver)
            }
        }
    }

    fun calculateAnnotationsForClassLike(designation: FirDeclarationDesignation) {
        val classLike = designation.declaration as FirClassLikeDeclaration
        if (classLike.annotations.isEmpty()) return
        val newAnnotations = RawFirNonLocalAnnotationsBuilder.build(
            session = classLike.moduleData.session,
            scopeProvider = classLike.moduleData.session.kotlinScopeProvider,
            designation = designation,
            rootNonLocalDeclaration = classLike.psi as KtDeclaration
        )
        replaceAnnotations(classLike.annotations, newAnnotations)
    }

    fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
        firProperty.getter?.body is FirLazyBlock
                || firProperty.setter?.body is FirLazyBlock
                || firProperty.initializer is FirLazyExpression
                || (firProperty.delegate as? FirWrappedDelegateExpression)?.expression is FirLazyExpression
                || firProperty.getExplicitBackingField()?.initializer is FirLazyExpression
                || needCalculatingForAnnotations(firProperty.annotations)
                || needCalculatingForAnnotations(firProperty.getter?.annotations)
                || needCalculatingForAnnotations(firProperty.setter?.annotations)

    fun needCalculatingForFunction(function: FirFunction): Boolean {
        if (function.body is FirLazyBlock || needCalculatingForAnnotations(function.annotations)) return true
        for (parameter in function.valueParameters) {
            if (parameter.defaultValue is FirLazyExpression) return true
        }
        return false
    }

    fun needCalculatingForConstructor(constructor: FirConstructor) =
        constructor.delegatedConstructor is FirLazyDelegatedConstructorCall || needCalculatingForFunction(constructor)

    fun needCalculatingForAnnotationCall(annotationCall: FirAnnotation): Boolean {
        if (annotationCall !is FirAnnotationCall) return false
        for (argument in annotationCall.argumentList.arguments) {
            if (argument is FirLazyExpression) return true
        }
        return false
    }

    fun needCalculatingForAnnotations(annotations: List<FirAnnotation>?): Boolean {
        if (annotations == null) return false
        for (annotation in annotations) {
            if (needCalculatingForAnnotationCall(annotation)) return true
        }
        return false
    }
}

private class FirLazyBodiesCalculatorTransformer(val contractableOnly: Boolean = false) : FirTransformer<PersistentList<FirDeclaration>>() {

    override fun transformFile(file: FirFile, data: PersistentList<FirDeclaration>): FirFile {
        file.declarations.forEach {
            it.transformSingle(this, data)
        }
        return file
    }

    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E {
        if (element is FirRegularClass) {
            val newList = data.add(element)
            element.declarations.forEach {
                it.transformSingle(this, newList)
            }
            element.transformChildren(this, newList)
        }
        if (element is FirClassLikeDeclaration && element.annotations.isNotEmpty()) {
            val designation = FirDeclarationDesignation(data, element)
            FirLazyBodiesCalculator.calculateAnnotationsForClassLike(designation)
        }
        return element
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirDeclaration>
    ): FirSimpleFunction {
        if (FirLazyBodiesCalculator.needCalculatingForFunction(simpleFunction)) {
            val designation = FirDeclarationDesignation(data, simpleFunction)
            FirLazyBodiesCalculator.calculateLazyBodiesForFunction(designation)
        }
        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirDeclaration>
    ): FirConstructor {
        if (!contractableOnly && FirLazyBodiesCalculator.needCalculatingForConstructor(constructor)) {
            val designation = FirDeclarationDesignation(data, constructor)
            if (constructor is FirPrimaryConstructor) {
                FirLazyBodiesCalculator.calculateLazyBodyForPrimaryConstructor(designation)
            } else if (!constructor.isPrimary) {
                FirLazyBodiesCalculator.calculateLazyBodyForSecondaryConstructor(designation)
            }
        }
        return constructor
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: PersistentList<FirDeclaration>
    ): FirAnonymousInitializer {
        if (!contractableOnly && anonymousInitializer.body is FirLazyBlock) {
            val designation = FirDeclarationDesignation(data, anonymousInitializer)
            FirLazyBodiesCalculator.calculateLazyBodyForAnonymousInitializer(designation)
        }
        return anonymousInitializer
    }

    override fun transformProperty(property: FirProperty, data: PersistentList<FirDeclaration>): FirProperty {
        if ((!contractableOnly || property.getter != null || property.setter != null) && FirLazyBodiesCalculator.needCalculatingLazyBodyForProperty(
                property
            )
        ) {
            val designation = FirDeclarationDesignation(data, property)
            FirLazyBodiesCalculator.calculateLazyBodyForProperty(designation)
        }
        return property
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: PersistentList<FirDeclaration>): FirStatement {
        if (!contractableOnly && enumEntry.initializer is FirLazyExpression) {
            val designation = FirDeclarationDesignation(data, enumEntry)
            FirLazyBodiesCalculator.calculateLazyInitializerForEnumEntry(designation)
        }
        return enumEntry
    }
}
