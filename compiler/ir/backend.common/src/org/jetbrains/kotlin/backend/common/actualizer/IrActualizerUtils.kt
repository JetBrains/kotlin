/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal fun collectActualCallablesMatchingToSpecificExpect(
    expectSymbol: IrSymbol,
    actualSymbols: List<IrSymbol>,
    expectToActualClassMap: Map<ClassId, IrClassSymbol>,
    typeSystemContext: IrTypeSystemContext,
): List<IrSymbol> {
    val baseExpectSymbol = expectSymbol
    val matchingActuals = mutableListOf<IrSymbol>()
    val context = object : IrExpectActualMatchingContext(typeSystemContext, expectToActualClassMap) {
        override fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol) {
            shouldNotBeCalled()
        }

        override fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            require(expectSymbol == baseExpectSymbol)
            matchingActuals += actualSymbol
        }
    }
    AbstractExpectActualCompatibilityChecker.matchSingleExpectTopLevelDeclarationAgainstPotentialActuals(
        expectSymbol,
        actualSymbols,
        context
    )
    return matchingActuals
}

internal fun recordActualForExpectDeclaration(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    destination: MutableMap<IrSymbol, IrSymbol>,
) {
    val expectDeclaration = expectSymbol.owner as IrDeclarationBase
    val actualDeclaration = actualSymbol.owner as IrDeclaration
    val registeredActual = destination.put(expectSymbol, actualSymbol)
    require(registeredActual == null || registeredActual == actualSymbol) {
        """
            Expect symbol already has registered mapping
            
            Expect declaration: ${expectDeclaration.render()}
            Actual declaration: ${actualDeclaration.render()}
            Already registered: ${registeredActual!!.owner.render()}
        """.trimIndent()
    }
    if (expectDeclaration is IrTypeParametersContainer) {
        recordTypeParametersMapping(destination, expectDeclaration, actualDeclaration as IrTypeParametersContainer)
    }
    if (expectDeclaration is IrProperty) {
        val actualProperty = actualDeclaration as IrProperty
        expectDeclaration.getter!!.let {
            val getter = actualProperty.getter!!
            destination[it.symbol] = getter.symbol
            recordTypeParametersMapping(destination, it, getter)
        }
        expectDeclaration.setter?.symbol?.let { destination[it] = actualProperty.setter!!.symbol }
    }
}

private fun recordTypeParametersMapping(
    destination: MutableMap<IrSymbol, IrSymbol>,
    expectTypeParametersContainer: IrTypeParametersContainer,
    actualTypeParametersContainer: IrTypeParametersContainer
) {
    expectTypeParametersContainer.typeParameters
        .zip(actualTypeParametersContainer.typeParameters)
        .forEach { (expectTypeParameter, actualTypeParameter) ->
            destination[expectTypeParameter.symbol] = actualTypeParameter.symbol
        }
}

internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportMissingActual(expectSymbol: IrSymbol) {
    reportMissingActual(expectSymbol.owner as IrDeclaration)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportMissingActual(irDeclaration: IrDeclaration) {
    at(irDeclaration).report(
        CommonBackendErrors.NO_ACTUAL_FOR_EXPECT,
        (irDeclaration as? IrDeclarationWithName)?.name?.asString().orEmpty(),
        irDeclaration.module
    )
}

internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportIncompatibleExpectActual(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    incompatibility: ExpectActualCompatibility.Incompatible<*>
) {
    val expectDeclaration = expectSymbol.owner as IrDeclaration
    val actualDeclaration = actualSymbol.owner as IrDeclaration
    at(expectDeclaration).report(
        CommonBackendErrors.INCOMPATIBLE_MATCHING,
        expectDeclaration.getNameWithAssert().asString(),
        actualDeclaration.getNameWithAssert().asString(),
        incompatibility
    )
}

internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportActualAnnotationsNotMatchExpect(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    incompatibilityType: ExpectActualAnnotationsIncompatibilityType<IrConstructorCall>,
    reportOn: IrSymbol,
) {
    at(reportOn.owner as IrDeclaration).report(
        CommonBackendErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
        expectSymbol,
        actualSymbol,
        incompatibilityType,
    )
}

internal fun KtDiagnosticReporterWithImplicitIrBasedContext.reportActualAnnotationConflictingDefaultArgumentValue(
    reportOn: IrElement,
    file: IrFile,
    actualParam: IrValueParameter,
) {
    at(reportOn, file).report(
        CommonBackendErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE,
        actualParam,
    )
}

internal fun IrElement.containsOptionalExpectation(): Boolean {
    return this is IrClass &&
            this.kind == ClassKind.ANNOTATION_CLASS &&
            this.hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)
}

@Suppress("UNCHECKED_CAST")
internal fun createFakeOverrideMember(actualMembers: List<IrDeclaration>, declaration: IrClass): IrOverridableDeclaration<*> {
    return when (actualMembers.first()) {
        is IrSimpleFunction -> createFakeOverrideFunction(actualMembers as List<IrSimpleFunction>, declaration)
        is IrProperty -> createFakeOverrideProperty(actualMembers as List<IrProperty>, declaration)
        else -> error("Only function or property can be overridden")
    }
}

private fun createFakeOverrideProperty(actualProperties: List<IrProperty>, parent: IrClass): IrProperty {
    val actualProperty = actualProperties.first() // TODO: Currently FIR2IR works in the similar way but it looks incorrect
    return parent.factory.buildProperty {
        updateFrom(actualProperty)
        name = actualProperty.name
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        isFakeOverride = true
        isExpect = false
    }.apply {
        this.parent = parent
        annotations = actualProperty.annotations
        backingField = null
        getter = createFakeOverrideFunction(actualProperties.map { it.getter as IrSimpleFunction }, parent, symbol)
        setter = runIf(actualProperty.setter != null) {
            createFakeOverrideFunction(actualProperties.map { it.setter as IrSimpleFunction }, parent, symbol)
        }
        overriddenSymbols = actualProperties.map { it.symbol }
    }
}

private fun createFakeOverrideFunction(
    actualFunctions: List<IrSimpleFunction>,
    parent: IrDeclarationParent,
    correspondingPropertySymbol: IrPropertySymbol? = null
): IrSimpleFunction {
    val actualFunction = actualFunctions.first() // TODO: Currently FIR2IR works in the similar way but it looks incorrect

    return actualFunction.factory.buildFun {
        updateFrom(actualFunction)
        name = actualFunction.name
        returnType = actualFunction.returnType
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        isFakeOverride = true
        isExpect = false
    }.also {
        it.parent = parent
        it.annotations = actualFunction.annotations.map { p -> p.deepCopyWithSymbols(it) }
        it.typeParameters = actualFunction.typeParameters.map { p -> p.deepCopyWithSymbols(it) }

        val typeRemapper = IrTypeParameterRemapper(actualFunction.typeParameters.zip(it.typeParameters).toMap())
        fun IrValueParameter.deepCopyWithTypeParameters(): IrValueParameter = deepCopyWithSymbols(it) { symbolRemapper, _ ->
            DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper)
        }

        it.dispatchReceiverParameter = actualFunction.dispatchReceiverParameter?.deepCopyWithTypeParameters()
        it.extensionReceiverParameter = actualFunction.extensionReceiverParameter?.deepCopyWithTypeParameters()
        it.valueParameters = actualFunction.valueParameters.map { p -> p.deepCopyWithTypeParameters() }
        it.contextReceiverParametersCount = actualFunction.contextReceiverParametersCount
        it.metadata = actualFunction.metadata
        it.overriddenSymbols = actualFunctions.map { f -> f.symbol }
        it.attributeOwnerId = it
        it.correspondingPropertySymbol = correspondingPropertySymbol
    }
}
