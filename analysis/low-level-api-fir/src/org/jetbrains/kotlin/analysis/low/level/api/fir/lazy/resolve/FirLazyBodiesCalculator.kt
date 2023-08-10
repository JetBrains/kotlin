/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.psi.*

internal object FirLazyBodiesCalculator {
    fun calculateBodies(designation: FirDesignation) {
        designation.target.transform<FirElement, PersistentList<FirRegularClass>>(
            FirTargetLazyBodiesCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    fun calculateAllLazyExpressionsInFile(firFile: FirFile) {
        firFile.transformSingle(FirAllLazyAnnotationCalculatorTransformer, FirLazyAnnotationTransformerData(firFile.moduleData.session))
        firFile.transformSingle(FirAllLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateAnnotations(firElement: FirElementWithResolveState) {
        calculateAnnotations(firElement, firElement.moduleData.session)
    }

    fun calculateAnnotations(firElement: FirElement, session: FirSession) {
        firElement.transformSingle(FirTargetLazyAnnotationCalculatorTransformer, FirLazyAnnotationTransformerData(session))
    }

    fun calculateCompilerAnnotations(firElement: FirElementWithResolveState) {
        firElement.transformSingle(
            FirTargetLazyAnnotationCalculatorTransformer,
            FirLazyAnnotationTransformerData(firElement.moduleData.session, FirLazyAnnotationTransformerScope.COMPILER_ONLY)
        )
    }

    fun calculateLazyArgumentsForAnnotation(annotationCall: FirAnnotationCall, session: FirSession): FirArgumentList {
        require(needCalculatingAnnotationCall(annotationCall))
        return createArgumentsForAnnotation(annotationCall, session)
    }

    fun createArgumentsForAnnotation(annotationCall: FirAnnotationCall, session: FirSession): FirArgumentList {
        val builder = PsiRawFirBuilder(session, baseScopeProvider = session.kotlinScopeProvider)
        val ktAnnotationEntry = annotationCall.psi as KtAnnotationEntry
        builder.context.packageFqName = ktAnnotationEntry.containingKtFile.packageFqName
        val newAnnotationCall = builder.buildAnnotationCall(ktAnnotationEntry)
        return newAnnotationCall.argumentList
    }

    fun createStatementsForScript(script: FirScript): List<FirStatement> {
        val newScript = revive<FirScript>(FirDesignation(emptyList(), script))
        return newScript.statements
    }

    fun needCalculatingAnnotationCall(firAnnotationCall: FirAnnotationCall): Boolean =
        firAnnotationCall.argumentList.arguments.any { it is FirLazyExpression }
}

private inline fun <reified T : FirDeclaration> revive(
    designation: FirDesignation,
    psiFactory: (FirDesignation) -> PsiElement? = { it.target.psi }
): T {
    val session = designation.target.moduleData.session

    return RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.kotlinScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = psiFactory(designation) as KtAnnotated,
    ) as T
}

private fun replaceLazyValueParameters(target: FirFunction, copy: FirFunction) {
    val targetParameters = target.valueParameters
    val copyParameters = copy.valueParameters
    require(targetParameters.size == copyParameters.size)

    for ((valueParameter, newValueParameter) in targetParameters.zip(copyParameters)) {
        if (valueParameter.defaultValue is FirLazyExpression) {
            valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
        }
    }
}

private fun replaceLazyBody(target: FirFunction, copy: FirFunction) {
    if (target.body is FirLazyBlock) {
        target.replaceBody(copy.body)
    }
}

private fun replaceLazyContractDescription(target: FirContractDescriptionOwner, copy: FirContractDescriptionOwner) {
    val shouldReplace = when (val currentContractDescription = target.contractDescription) {
        is FirRawContractDescription -> currentContractDescription.rawEffects.any { it is FirLazyExpression }
        is FirEmptyContractDescription -> copy.contractDescription !is FirEmptyContractDescription
        else -> false
    }

    if (shouldReplace) {
        target.replaceContractDescription(copy.contractDescription)
    }
}

private fun replaceLazyDelegatedConstructor(target: FirConstructor, copy: FirConstructor) {
    val targetCall = target.delegatedConstructor
    val copyCall = copy.delegatedConstructor

    when (targetCall) {
        is FirLazyDelegatedConstructorCall -> {
            require(copyCall !is FirMultiDelegatedConstructorCall)
            target.replaceDelegatedConstructor(copyCall)
        }
        is FirMultiDelegatedConstructorCall -> {
            require(copyCall is FirMultiDelegatedConstructorCall)
            require(targetCall.delegatedConstructorCalls.size == copyCall.delegatedConstructorCalls.size)

            val newCalls = targetCall.delegatedConstructorCalls.zip(copyCall.delegatedConstructorCalls)
                .map { (target, copy) -> target.takeUnless { it is FirLazyDelegatedConstructorCall } ?: copy }

            targetCall.replaceDelegatedConstructorCalls(newCalls)
        }
    }
}

private fun replaceLazyInitializer(target: FirVariable, copy: FirVariable) {
    if (target.initializer is FirLazyExpression) {
        target.replaceInitializer(copy.initializer)
    }
}

private fun replaceLazyDelegate(target: FirVariable, copy: FirVariable) {
    if (target.delegate is FirLazyExpression) {
        target.replaceDelegate(copy.delegate)
    }
}

private fun calculateLazyBodiesForFunction(designation: FirDesignation) {
    val simpleFunction = designation.target as FirSimpleFunction
    require(needCalculatingLazyBodyForFunction(simpleFunction))

    val newSimpleFunction = revive<FirSimpleFunction>(designation)

    replaceLazyContractDescription(simpleFunction, newSimpleFunction)
    replaceLazyBody(simpleFunction, newSimpleFunction)
    replaceLazyValueParameters(simpleFunction, newSimpleFunction)
}

private fun calculateLazyBodyForConstructor(designation: FirDesignation) {
    val constructor = designation.target as FirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))

    val newConstructor = revive<FirConstructor>(designation)

    replaceLazyContractDescription(constructor, newConstructor)
    replaceLazyBody(constructor, newConstructor)
    replaceLazyDelegatedConstructor(constructor, newConstructor)
    replaceLazyValueParameters(constructor, newConstructor)
}

private fun calculateLazyBodyForProperty(designation: FirDesignation) {
    val firProperty = designation.target as FirProperty
    if (!needCalculatingLazyBodyForProperty(firProperty)) return

    val newProperty = revive<FirProperty>(designation)

    firProperty.getter?.let { getter ->
        val newGetter = newProperty.getter!!
        replaceLazyContractDescription(getter, newGetter)
        replaceLazyBody(getter, newGetter)
    }

    firProperty.setter?.let { setter ->
        val newSetter = newProperty.setter!!
        replaceLazyContractDescription(setter, newSetter)
        replaceLazyBody(setter, newSetter)
    }

    replaceLazyInitializer(firProperty, newProperty)
    replaceLazyDelegate(firProperty, newProperty)

    firProperty.getExplicitBackingField()?.let { backingField ->
        val newBackingField = newProperty.getExplicitBackingField()!!
        replaceLazyInitializer(backingField, newBackingField)
    }
}

private fun calculateLazyInitializerForEnumEntry(designation: FirDesignation) {
    val enumEntry = designation.target as FirEnumEntry
    require(enumEntry.initializer is FirLazyExpression)

    val newEnumEntry = revive<FirEnumEntry>(designation)
    enumEntry.replaceInitializer(newEnumEntry.initializer)
}

private fun calculateLazyBodyForAnonymousInitializer(designation: FirDesignation) {
    val initializer = designation.target as FirAnonymousInitializer
    require(initializer.body is FirLazyBlock)

    val newInitializer = revive<FirAnonymousInitializer>(designation)
    initializer.replaceBody(newInitializer.body)
}

private fun needCalculatingLazyBodyForConstructor(firConstructor: FirConstructor): Boolean {
    if (needCalculatingLazyBodyForFunction(firConstructor) || firConstructor.delegatedConstructor is FirLazyDelegatedConstructorCall) {
        return true
    }
    val delegatedConstructor = firConstructor.delegatedConstructor
    if (delegatedConstructor is FirMultiDelegatedConstructorCall) {
        for (delegated in delegatedConstructor.delegatedConstructorCalls) {
            if (delegated is FirLazyDelegatedConstructorCall) {
                return true
            }
        }
    }
    return false
}

private fun calculateLazyBodiesForField(designation: FirDesignation) {
    val field = designation.target as FirField
    require(field.initializer is FirLazyExpression)

    val newField = revive<FirField>(designation) { it.path.last().psi }
    field.replaceInitializer(newField.initializer)
}

private fun needCalculatingLazyBodyForContractDescriptionOwner(firContractOwner: FirContractDescriptionOwner): Boolean {
    val contractDescription = firContractOwner.contractDescription
    if (contractDescription is FirRawContractDescription) {
        return contractDescription.rawEffects.any { it is FirLazyExpression }
    }

    return false
}

private fun needCalculatingLazyBodyForFunction(firFunction: FirFunction): Boolean {
    return (firFunction.body is FirLazyBlock
            || firFunction.valueParameters.any { it.defaultValue is FirLazyExpression })
            || (firFunction is FirContractDescriptionOwner && needCalculatingLazyBodyForContractDescriptionOwner(firFunction))
}

private fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
    firProperty.getter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.setter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.initializer is FirLazyExpression
            || firProperty.delegate is FirLazyExpression
            || firProperty.getExplicitBackingField()?.initializer is FirLazyExpression

private fun calculateLazyBodyForCodeFragment(designation: FirDesignation) {
    val codeFragment = designation.target as FirCodeFragment
    require(codeFragment.block is FirLazyBlock)

    val newCodeFragment = revive<FirCodeFragment>(designation)
    codeFragment.replaceBlock(newCodeFragment.block)
}

private enum class FirLazyAnnotationTransformerScope {
    ALL_ANNOTATIONS,
    COMPILER_ONLY;
}

private data class FirLazyAnnotationTransformerData(
    val session: FirSession,
    val compilerAnnotationsOnly: FirLazyAnnotationTransformerScope = FirLazyAnnotationTransformerScope.ALL_ANNOTATIONS,
)

private object FirAllLazyAnnotationCalculatorTransformer : FirLazyAnnotationTransformer() {
    override fun <E : FirElement> transformElement(element: E, data: FirLazyAnnotationTransformerData): E {
        element.transformChildren(this, data)
        return element
    }
}

private object FirTargetLazyAnnotationCalculatorTransformer : FirLazyAnnotationTransformer() {
    override fun <E : FirElement> transformElement(element: E, data: FirLazyAnnotationTransformerData): E {
        element.transformChildren(this, data)
        return element
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: FirLazyAnnotationTransformerData): FirStatement {
        regularClass.transformAnnotations(this, data)
        regularClass.transformTypeParameters(this, data)
        regularClass.transformSuperTypeRefs(this, data)
        regularClass.contextReceivers.forEach {
            it.transformSingle(this, data)
        }

        return regularClass
    }

    override fun transformBlock(block: FirBlock, data: FirLazyAnnotationTransformerData): FirStatement {
        // We shouldn't process blocks because there are no lazy annotations
        return block
    }
}

private abstract class FirLazyAnnotationTransformer : FirTransformer<FirLazyAnnotationTransformerData>() {
    override fun <E : FirElement> transformElement(element: E, data: FirLazyAnnotationTransformerData): E {
        element.transformChildren(this, data)
        return element
    }

    private fun canBeCompilerAnnotation(annotationCall: FirAnnotationCall, session: FirSession): Boolean {
        val annotationTypeRef = annotationCall.annotationTypeRef
        if (annotationTypeRef !is FirUserTypeRef) return false
        if (session.registeredPluginAnnotations.annotations.isNotEmpty()) return true
        val name = annotationTypeRef.qualifier.last().name
        return name in session.annotationPlatformSupport.requiredAnnotationsShortClassNames
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: FirLazyAnnotationTransformerData): FirTypeRef {
        resolvedTypeRef.coneType.forEachType { coneType ->
            for (typeArgumentAnnotation in coneType.customAnnotations) {
                typeArgumentAnnotation.accept(this, data)
            }
        }

        return super.transformResolvedTypeRef(resolvedTypeRef, data)
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: FirLazyAnnotationTransformerData): FirStatement {
        val shouldCalculate = data.compilerAnnotationsOnly == FirLazyAnnotationTransformerScope.ALL_ANNOTATIONS ||
                canBeCompilerAnnotation(annotationCall, data.session)
        if (shouldCalculate && FirLazyBodiesCalculator.needCalculatingAnnotationCall(annotationCall)) {
            val newArgumentList = FirLazyBodiesCalculator.calculateLazyArgumentsForAnnotation(annotationCall, data.session)
            annotationCall.replaceArgumentList(newArgumentList)
        }

        super.transformAnnotationCall(annotationCall, data)
        return annotationCall
    }

    override fun transformErrorAnnotationCall(
        errorAnnotationCall: FirErrorAnnotationCall,
        data: FirLazyAnnotationTransformerData,
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
        data: FirLazyAnnotationTransformerData,
    ): FirStatement {
        if (delegatedConstructorCall is FirLazyDelegatedConstructorCall) {
            return delegatedConstructorCall
        }

        return super.transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }
}

private object FirAllLazyBodiesCalculatorTransformer : FirLazyBodiesCalculatorTransformer() {
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
}

private object FirTargetLazyBodiesCalculatorTransformer : FirLazyBodiesCalculatorTransformer()

private abstract class FirLazyBodiesCalculatorTransformer : FirTransformer<PersistentList<FirRegularClass>>() {
    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirRegularClass>): E = element

    override fun transformField(field: FirField, data: PersistentList<FirRegularClass>): FirStatement {
        if (field.initializer is FirLazyExpression) {
            val designation = FirDesignation(data, field)
            calculateLazyBodiesForField(designation)
        }

        return field
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirRegularClass>,
    ): FirSimpleFunction {
        if (needCalculatingLazyBodyForFunction(simpleFunction)) {
            val designation = FirDesignation(data, simpleFunction)
            calculateLazyBodiesForFunction(designation)
        }

        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirRegularClass>,
    ): FirConstructor {
        if (needCalculatingLazyBodyForConstructor(constructor)) {
            val designation = FirDesignation(data, constructor)
            calculateLazyBodyForConstructor(designation)
        }

        return constructor
    }

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: PersistentList<FirRegularClass>) =
        transformConstructor(errorPrimaryConstructor, data)

    override fun transformProperty(property: FirProperty, data: PersistentList<FirRegularClass>): FirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            val designation = FirDesignation(data, property)
            calculateLazyBodyForProperty(designation)
        }

        return property
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: PersistentList<FirRegularClass>): FirStatement {
        return propertyAccessor.also { transformProperty(it.propertySymbol.fir, data) }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: PersistentList<FirRegularClass>): FirStatement {
        if (enumEntry.initializer is FirLazyExpression) {
            val designation = FirDesignation(data, enumEntry)
            calculateLazyInitializerForEnumEntry(designation)
        }

        return enumEntry
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer, data: PersistentList<FirRegularClass>,
    ): FirAnonymousInitializer {
        if (anonymousInitializer.body is FirLazyBlock) {
            val designation = FirDesignation(data, anonymousInitializer)
            calculateLazyBodyForAnonymousInitializer(designation)
        }

        return anonymousInitializer
    }

    override fun transformCodeFragment(codeFragment: FirCodeFragment, data: PersistentList<FirRegularClass>): FirCodeFragment {
        if (codeFragment.block is FirLazyBlock) {
            val designation = FirDesignation(data, codeFragment)
            calculateLazyBodyForCodeFragment(designation)
        }

        return codeFragment
    }
}
