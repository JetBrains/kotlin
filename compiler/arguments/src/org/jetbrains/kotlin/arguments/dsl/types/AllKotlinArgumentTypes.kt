/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.contextual
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.base.allNamedTypeSerializerWithVersions
import kotlin.reflect.KClass

/**
 * Class containing all non-primitive compiler argument types which are serialized in more detailed form.
 */
@OptIn(ExperimentalArgumentApi::class)
@Suppress("unused")
@Serializable
class AllKotlinArgumentTypes {
    @Serializable
    val kotlinVersions: TypeWithEntries<KotlinVersionWithReleaseVersions> = KotlinVersionType::class with KotlinVersion.entries.toSet()

    @Serializable
    val jvmTargets: TypeWithEntries<JvmTargetWithReleaseVersions> =
        KotlinJvmTargetType::class with JvmTarget.entries.toSet()

    @Serializable
    val explicitApiModes: TypeWithEntries<ExplicitApiModeWithReleaseVersions> =
        KotlinExplicitApiModeType::class with ExplicitApiMode.entries.toSet()

    @Serializable
    val returnValueCheckerMode: TypeWithEntries<ReturnValueCheckerModeWithReleaseVersions> =
        ReturnValueCheckerModeType::class with ReturnValueCheckerMode.entries.toSet()

    @Serializable
    val klibIrInlinerMode: TypeWithEntries<KlibIrInlinerModeWithReleaseVersions> =
        KlibIrInlinerModeType::class with KlibIrInlinerMode.entries.toSet()

    @Serializable
    val jvmDefaultMode: TypeWithEntries<JvmDefaultModeWithReleaseVersions> =
        JvmDefaultModeType::class with JvmDefaultMode.entries.toSet()

    @Serializable
    val abiStabilityMode: TypeWithEntries<AbiStabilityModeWithReleaseVersions> =
        AbiStabilityModeType::class with AbiStabilityMode.entries.toSet()

    @Serializable
    val assertionsMode: TypeWithEntries<AssertionsModeWithReleaseVersions> =
        AssertionsModeType::class with AssertionsMode.entries.toSet()

    @Serializable
    val jspecifyAnnotationsMode: TypeWithEntries<JspecifyAnnotationsModeWithReleaseVersions> =
        JspecifyAnnotationsModeType::class with JspecifyAnnotationsMode.entries.toSet()

    @Serializable
    val lambdasMode: TypeWithEntries<LambdasModeWithReleaseVersions> =
        LambdasModeType::class with LambdasMode.entries.toSet()

    @Serializable
    val samConversionsMode: TypeWithEntries<SamConversionsModeWithReleaseVersions> =
        SamConversionsModeType::class with SamConversionsMode.entries.toSet()

    @Serializable
    val stringConcatMode: TypeWithEntries<StringConcatModeWithReleaseVersions> =
        StringConcatModeType::class with StringConcatMode.entries.toSet()

    @Serializable
    val compatqualAnnotationsMode: TypeWithEntries<CompatqualAnnotationsModeWithReleaseVersions> =
        CompatqualAnnotationsModeType::class with CompatqualAnnotationsMode.entries.toSet()

    @Serializable
    val whenExpressionsMode: TypeWithEntries<WhenExpressionsModeWithReleaseVersions> =
        WhenExpressionsModeType::class with WhenExpressionsMode.entries.toSet()

    @Serializable
    val jdkRelease: TypeWithEntries<JdkReleaseWithReleaseVersions> =
        JdkReleaseType::class with JdkRelease.entries.toSet()

    @Serializable
    val annotationDefaultTarget: TypeWithEntries<AnnotationDefaultTargetModeWithReleaseVersions> =
        AnnotationDefaultTargetModeType::class with AnnotationDefaultTargetMode.entries.toSet()

    @Serializable
    val nameBasedDestructuring: TypeWithEntries<NameBasedDestructuringModeWithReleaseVersions> =
        NameBasedDestructuringModeType::class with NameBasedDestructuringMode.entries.toSet()

    @Serializable
    val verifyIrMode: TypeWithEntries<VerifyIrModeWithReleaseVersions> =
        VerifyIrModeType::class with VerifyIrMode.entries.toSet()

    @Serializable
    val partialLinkageMode: TypeWithEntries<PartialLinkageModeWithReleaseVersions> =
        PartialLinkageModeType::class with PartialLinkageMode.entries.toSet()

    @Serializable
    val partialLinkageLogLevel: TypeWithEntries<PartialLinkageLogLevelWithReleaseVersions> =
        PartialLinkageLogLevelType::class with PartialLinkageLogLevel.entries.toSet()

    @Serializable
    val duplicatedUniqueNameStrategy: TypeWithEntries<DuplicatedUniqueNameStrategyWithReleaseVersions> =
        DuplicatedUniqueNameStrategyType::class with DuplicatedUniqueNameStrategy.entries.toSet()

    @Serializable
    val jsEcmaVersion: TypeWithEntries<JsEcmaVersionWithReleaseVersions> =
        JsEcmaVersionType::class with JsEcmaVersion.entries.toSet()

    @Serializable
    val jsModuleKind: TypeWithEntries<JsModuleKindWithReleaseVersions> =
        JsModuleKindType::class with JsModuleKind.entries.toSet()

    @Serializable
    val jsIrDiagnosticMode: TypeWithEntries<JsIrDiagnosticModeWithReleaseVersions> =
        JsIrDiagnosticModeType::class with JsIrDiagnosticMode.entries.toSet()

    @Serializable
    val jsMainCallMode: TypeWithEntries<JsMainCallModeWithReleaseVersions> =
        JsMainCallModeType::class with JsMainCallMode.entries.toSet()

    @Serializable
    val sourceMapEmbedSources: TypeWithEntries<SourceMapEmbedSourcesWithReleaseVersions> =
        SourceMapEmbedSourcesType::class with SourceMapEmbedSources.entries.toSet()

    @Serializable
    val sourceMapNamesPolicy: TypeWithEntries<SourceMapNamesPolicyWithReleaseVersions> =
        SourceMapNamesPolicyType::class with SourceMapNamesPolicy.entries.toSet()

    @Serializable
    val wasmTarget: TypeWithEntries<WasmTargetWithReleaseVersions> =
        WasmTargetType::class with WasmTarget.entries.toSet()

}

@Serializable
class TypeWithEntries<T>(
    val type: String,
    val values: Set<T>,
)

infix fun <T> KClass<out EnumType<T>>.with(entries: Set<T>) where T : Enum<T>, T : WithStringRepresentation =
    TypeWithEntries(this.java.name, entries)

val allKotlinTypeSerializersModule = SerializersModule {
    contextualSerializerWithVersions<KotlinVersionWithReleaseVersions>()
    contextualSerializerWithVersions<JvmTargetWithReleaseVersions>()
    contextualSerializerWithVersions<ExplicitApiModeWithReleaseVersions>()
    contextualSerializerWithVersions<ReturnValueCheckerModeWithReleaseVersions>()
    contextualSerializerWithVersions<KlibIrInlinerModeWithReleaseVersions>()
    contextualSerializerWithVersions<JvmDefaultModeWithReleaseVersions>()
    contextualSerializerWithVersions<AbiStabilityModeWithReleaseVersions>()
    contextualSerializerWithVersions<AssertionsModeWithReleaseVersions>()
    contextualSerializerWithVersions<JspecifyAnnotationsModeWithReleaseVersions>()
    contextualSerializerWithVersions<LambdasModeWithReleaseVersions>()
    contextualSerializerWithVersions<SamConversionsModeWithReleaseVersions>()
    contextualSerializerWithVersions<StringConcatModeWithReleaseVersions>()
    contextualSerializerWithVersions<CompatqualAnnotationsModeWithReleaseVersions>()
    contextualSerializerWithVersions<WhenExpressionsModeWithReleaseVersions>()
    contextualSerializerWithVersions<JdkReleaseWithReleaseVersions>()
    contextualSerializerWithVersions<AnnotationDefaultTargetModeWithReleaseVersions>()
    contextualSerializerWithVersions<NameBasedDestructuringModeWithReleaseVersions>()
    contextualSerializerWithVersions<VerifyIrModeWithReleaseVersions>()
    contextualSerializerWithVersions<PartialLinkageModeWithReleaseVersions>()
    contextualSerializerWithVersions<PartialLinkageLogLevelWithReleaseVersions>()
    contextualSerializerWithVersions<DuplicatedUniqueNameStrategyWithReleaseVersions>()
    contextualSerializerWithVersions<JsEcmaVersionWithReleaseVersions>()
    contextualSerializerWithVersions<JsModuleKindWithReleaseVersions>()
    contextualSerializerWithVersions<JsIrDiagnosticModeWithReleaseVersions>()
    contextualSerializerWithVersions<JsMainCallModeWithReleaseVersions>()
    contextualSerializerWithVersions<SourceMapEmbedSourcesWithReleaseVersions>()
    contextualSerializerWithVersions<SourceMapNamesPolicyWithReleaseVersions>()
    contextualSerializerWithVersions<WasmTargetWithReleaseVersions>()
}

inline fun <reified T> SerializersModuleBuilder.contextualSerializerWithVersions() where T : WithKotlinReleaseVersionsMetadata, T : Enum<T>, T : WithStringRepresentation {
    contextual(allNamedTypeSerializerWithVersions<T>())
}
