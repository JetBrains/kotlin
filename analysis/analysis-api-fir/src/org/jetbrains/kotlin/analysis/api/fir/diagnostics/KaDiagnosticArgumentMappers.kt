/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaUnstableDiagnosticApi::class)

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData

/**
 * Converts the effective visibility to the closest [KaSymbolVisibility], as [EffectiveVisibility.toVisibility] does.
 */
internal fun EffectiveVisibility.toKaSymbolVisibility(): KaSymbolVisibility = when (this) {
    EffectiveVisibility.Public -> KaSymbolVisibility.PUBLIC
    is EffectiveVisibility.Protected, EffectiveVisibility.ProtectedBound -> KaSymbolVisibility.PROTECTED
    EffectiveVisibility.Internal -> KaSymbolVisibility.INTERNAL
    EffectiveVisibility.PackagePrivate -> KaSymbolVisibility.PACKAGE_PRIVATE
    is EffectiveVisibility.InternalProtected, EffectiveVisibility.InternalProtectedBound -> KaSymbolVisibility.PRIVATE
    EffectiveVisibility.PrivateInFile, EffectiveVisibility.PrivateInClass -> KaSymbolVisibility.PRIVATE
    EffectiveVisibility.Local -> KaSymbolVisibility.LOCAL
    EffectiveVisibility.Unknown -> KaSymbolVisibility.UNKNOWN
}

internal fun RelationToType.toKaRelationToType(): KaRelationToType = when (this) {
    RelationToType.CONSTRUCTOR -> KaRelationToType.CONSTRUCTOR
    RelationToType.CONTAINER -> KaRelationToType.CONTAINER
    RelationToType.ARGUMENT -> KaRelationToType.ARGUMENT
    RelationToType.ARGUMENT_CONTAINER -> KaRelationToType.ARGUMENT_CONTAINER
}

internal fun ClassKind.toKaClassKind(): KaClassKind = when (this) {
    // An enum entry with a body is an anonymous object for the Analysis API
    ClassKind.ENUM_ENTRY -> KaClassKind.ANONYMOUS_OBJECT
    else -> toKtClassKind(isCompanionObject = false)
}

internal fun ForbiddenNamedArgumentsTarget.toKaForbiddenNamedArgumentsTarget(): KaForbiddenNamedArgumentsTarget = when (this) {
    ForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION -> KaForbiddenNamedArgumentsTarget.NON_KOTLIN_FUNCTION
    ForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE -> KaForbiddenNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
    ForbiddenNamedArgumentsTarget.EXPECTED_CLASS_MEMBER -> KaForbiddenNamedArgumentsTarget.EXPECTED_CLASS_MEMBER
}

internal fun ExpectActualMatchingCompatibility.toKaExpectActualMatchingCompatibility(): KaExpectActualMatchingCompatibility = when (this) {
    ExpectActualMatchingCompatibility.CallableKind -> KaExpectActualMatchingCompatibility.CALLABLE_KIND
    ExpectActualMatchingCompatibility.ActualJavaField -> KaExpectActualMatchingCompatibility.ACTUAL_JAVA_FIELD
    ExpectActualMatchingCompatibility.ParameterShape -> KaExpectActualMatchingCompatibility.PARAMETER_SHAPE
    ExpectActualMatchingCompatibility.ParameterCount -> KaExpectActualMatchingCompatibility.PARAMETER_COUNT
    ExpectActualMatchingCompatibility.ContextParameterCount -> KaExpectActualMatchingCompatibility.CONTEXT_PARAMETER_COUNT
    ExpectActualMatchingCompatibility.FunctionTypeParameterCount -> KaExpectActualMatchingCompatibility.FUNCTION_TYPE_PARAMETER_COUNT
    ExpectActualMatchingCompatibility.ParameterTypes -> KaExpectActualMatchingCompatibility.PARAMETER_TYPES
    ExpectActualMatchingCompatibility.ParameterNames -> KaExpectActualMatchingCompatibility.PARAMETER_NAMES
    ExpectActualMatchingCompatibility.ContextParameterTypes -> KaExpectActualMatchingCompatibility.CONTEXT_PARAMETER_TYPES
    ExpectActualMatchingCompatibility.FunctionTypeParameterUpperBounds ->
        KaExpectActualMatchingCompatibility.FUNCTION_TYPE_PARAMETER_UPPER_BOUNDS
    ExpectActualMatchingCompatibility.MatchedSuccessfully -> KaExpectActualMatchingCompatibility.MATCHED_SUCCESSFULLY
}

internal fun ExpectActualAnnotationsIncompatibilityType<FirAnnotation>.toKaExpectActualAnnotationsIncompatibilityType(
    builder: KaSymbolByFirBuilder,
): KaExpectActualAnnotationsIncompatibilityType = when (this) {
    is ExpectActualAnnotationsIncompatibilityType.MissingOnActual ->
        KaExpectActualAnnotationsIncompatibilityType.MissingOnActual(expectAnnotation.toKaAnnotation(builder))
    is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual ->
        KaExpectActualAnnotationsIncompatibilityType.DifferentOnActual(
            expectAnnotation.toKaAnnotation(builder),
            actualAnnotation.toKaAnnotation(builder),
        )
}

internal fun IncompatibleVersionErrorData<*>.toKaIncompatibleVersionErrorData(): KaIncompatibleVersionErrorData {
    return KaIncompatibleVersionErrorData(
        actualVersion = actualVersion.toString(),
        compilerVersion = compilerVersion.toString(),
        languageVersion = languageVersion.toString(),
        expectedVersion = expectedVersion.toString(),
        filePath = filePath,
    )
}

internal fun VersionRequirement.Version.toMavenComparableVersion(): MavenComparableVersion = MavenComparableVersion(asString())

internal fun FirModuleData.toKaModule(): KaModule = (this as LLFirModuleData).ktModule
