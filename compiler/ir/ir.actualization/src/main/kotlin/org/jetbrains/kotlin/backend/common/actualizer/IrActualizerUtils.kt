/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

internal fun recordActualForExpectDeclaration(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    expectActualMap: IrExpectActualMap,
    diagnosticsReporter: IrDiagnosticReporter,
) {
    val expectDeclaration = expectSymbol.owner as IrDeclarationBase
    val actualDeclaration = actualSymbol.owner as IrDeclaration
    val registeredActual = expectActualMap.regularSymbols.put(expectSymbol, actualSymbol)
    if (registeredActual != null && registeredActual != actualSymbol) {
        diagnosticsReporter.reportAmbiguousActuals(expectDeclaration)
    }
    if (expectDeclaration is IrTypeParametersContainer) {
        recordTypeParametersMapping(expectActualMap, expectDeclaration, actualDeclaration as IrTypeParametersContainer)
    }
    if (expectDeclaration is IrProperty) {
        require(actualDeclaration is IrProperty)

        expectDeclaration.getter?.let { expectGetter ->
            val actualGetter = actualDeclaration.getter
            if (actualGetter != null) {
                expectActualMap.regularSymbols[expectGetter.symbol] = actualGetter.symbol
                recordTypeParametersMapping(expectActualMap, expectGetter, actualGetter)
            } else if (actualDeclaration.isPropertyForJavaField()) {
                // In the case when expect property is actualized by a Java field, there is no getter.
                // So, record it in `IrExpectActualMap.propertyAccessorsActualizedByFields`.
                expectActualMap.propertyAccessorsActualizedByFields[expectGetter.symbol] = actualDeclaration.symbol
            } else {
                error("Actual property ${actualDeclaration.render()} has not getter while expect property ${expectDeclaration.render()} has it")
            }
        }

        expectDeclaration.setter?.let { expectSetter ->
            val actualSetter = actualDeclaration.setter
            if (actualSetter != null) {
                expectActualMap.regularSymbols[expectSetter.symbol] = actualSetter.symbol
            } else if (actualDeclaration.isPropertyForJavaField()) {
                // In the case when expect property is actualized by a Java field, there is no setter.
                // So, record it in `IrExpectActualMap.propertyAccessorsActualizedByFields`.
                expectActualMap.propertyAccessorsActualizedByFields[expectSetter.symbol] = actualDeclaration.symbol
            } else {
                error("Actual property ${actualDeclaration.render()} has not setter while expect property ${expectDeclaration.render()} has it")
            }
        }
    }
}

private fun recordTypeParametersMapping(
    expectActualMap: IrExpectActualMap,
    expectTypeParametersContainer: IrTypeParametersContainer,
    actualTypeParametersContainer: IrTypeParametersContainer
) {
    expectTypeParametersContainer.typeParameters
        .zip(actualTypeParametersContainer.typeParameters)
        .forEach { (expectTypeParameter, actualTypeParameter) ->
            expectActualMap.regularSymbols[expectTypeParameter.symbol] = actualTypeParameter.symbol
        }
}

internal fun IrDiagnosticReporter.reportMissingActual(expectSymbol: IrSymbol) {
    reportMissingActual(expectSymbol.owner as IrDeclaration)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrDiagnosticReporter.reportMissingActual(irDeclaration: IrDeclaration) {
    at(irDeclaration).report(
        IrActualizationErrors.NO_ACTUAL_FOR_EXPECT,
        (irDeclaration as? IrDeclarationWithName)?.name?.asString().orEmpty(),
        irDeclaration.module
    )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrDiagnosticReporter.reportAmbiguousActuals(expectSymbol: IrDeclaration) {
    at(expectSymbol).report(
        IrActualizationErrors.AMBIGUOUS_ACTUALS,
        (expectSymbol as? IrDeclarationWithName)?.name?.asString().orEmpty(),
        expectSymbol.module
    )
}

internal fun IrDiagnosticReporter.reportExpectActualIncompatibility(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    incompatibility: ExpectActualCheckingCompatibility.Incompatible<*>,
) {
    val expectDeclaration = expectSymbol.owner as IrDeclaration
    val actualDeclaration = actualSymbol.owner as IrDeclaration
    at(expectDeclaration).report(
        IrActualizationErrors.EXPECT_ACTUAL_INCOMPATIBILITY,
        expectDeclaration.getNameWithAssert().asString(),
        actualDeclaration.getNameWithAssert().asString(),
        incompatibility
    )
}

internal fun IrDiagnosticReporter.reportExpectActualMismatch(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    incompatibility: ExpectActualMatchingCompatibility.Mismatch,
) {
    val expectDeclaration = expectSymbol.owner as IrDeclaration
    val actualDeclaration = actualSymbol.owner as IrDeclaration
    at(expectDeclaration).report(
        IrActualizationErrors.EXPECT_ACTUAL_MISMATCH,
        expectDeclaration.getNameWithAssert().asString(),
        actualDeclaration.getNameWithAssert().asString(),
        incompatibility
    )
}

internal fun IrDiagnosticReporter.reportActualAnnotationsNotMatchExpect(
    expectSymbol: IrSymbol,
    actualSymbol: IrSymbol,
    incompatibilityType: ExpectActualAnnotationsIncompatibilityType<IrConstructorCall>,
    reportOn: IrSymbol,
) {
    at(reportOn.owner as IrDeclaration).report(
        IrActualizationErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
        expectSymbol,
        actualSymbol,
        incompatibilityType,
    )
}

internal fun IrDiagnosticReporter.reportActualAnnotationConflictingDefaultArgumentValue(
    reportOn: IrElement,
    file: IrFile,
    actualParam: IrValueParameter,
) {
    at(reportOn, file).report(
        IrActualizationErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE,
        actualParam,
    )
}

internal fun IrElement.containsOptionalExpectation(): Boolean {
    return this is IrClass &&
            this.kind == ClassKind.ANNOTATION_CLASS &&
            this.hasAnnotation(StandardClassIds.Annotations.OptionalExpectation)
}

/**
 * Properties created for Java fields have non-null backing field, but don't have accessors.
 */
internal fun IrProperty.isPropertyForJavaField(): Boolean {
    return getter == null && setter == null && backingField != null
}
