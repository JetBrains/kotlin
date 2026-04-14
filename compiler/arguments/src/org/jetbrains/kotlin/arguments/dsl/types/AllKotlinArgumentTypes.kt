/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Contextual
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
    val kotlinVersions: TypeWithEntries<@Contextual KotlinVersion> = KotlinVersionType::class with KotlinVersion.entries.toSet()

    @Serializable
    val jvmTargets: TypeWithEntries<@Contextual JvmTarget> =
        KotlinJvmTargetType::class with JvmTarget.entries.toSet()

    @Serializable
    val explicitApiModes: TypeWithEntries<@Contextual ExplicitApiMode> =
        KotlinExplicitApiModeType::class with ExplicitApiMode.entries.toSet()

    @Serializable
    val returnValueCheckerMode: TypeWithEntries<@Contextual ReturnValueCheckerMode> =
        ReturnValueCheckerModeType::class with ReturnValueCheckerMode.entries.toSet()

    @Serializable
    val klibIrInlinerMode: TypeWithEntries<@Contextual KlibIrInlinerMode> =
        KlibIrInlinerModeType::class with KlibIrInlinerMode.entries.toSet()

    @Serializable
    val jvmDefaultMode: TypeWithEntries<@Contextual JvmDefaultMode> =
        JvmDefaultModeType::class with JvmDefaultMode.entries.toSet()

    @Serializable
    val abiStabilityMode: TypeWithEntries<@Contextual AbiStabilityMode> =
        AbiStabilityModeType::class with AbiStabilityMode.entries.toSet()

    @Serializable
    val assertionsMode: TypeWithEntries<@Contextual AssertionsMode> =
        AssertionsModeType::class with AssertionsMode.entries.toSet()

    @Serializable
    val jspecifyAnnotationsMode: TypeWithEntries<@Contextual JspecifyAnnotationsMode> =
        JspecifyAnnotationsModeType::class with JspecifyAnnotationsMode.entries.toSet()

    @Serializable
    val lambdasMode: TypeWithEntries<@Contextual LambdasMode> =
        LambdasModeType::class with LambdasMode.entries.toSet()

    @Serializable
    val samConversionsMode: TypeWithEntries<@Contextual SamConversionsMode> =
        SamConversionsModeType::class with SamConversionsMode.entries.toSet()

    @Serializable
    val stringConcatMode: TypeWithEntries<@Contextual StringConcatMode> =
        StringConcatModeType::class with StringConcatMode.entries.toSet()

    @Serializable
    val compatqualAnnotationsMode: TypeWithEntries<@Contextual CompatqualAnnotationsMode> =
        CompatqualAnnotationsModeType::class with CompatqualAnnotationsMode.entries.toSet()

    @Serializable
    val whenExpressionsMode: TypeWithEntries<@Contextual WhenExpressionsMode> =
        WhenExpressionsModeType::class with WhenExpressionsMode.entries.toSet()

    @Serializable
    val jdkRelease: TypeWithEntries<@Contextual JdkRelease> =
        JdkReleaseType::class with JdkRelease.entries.toSet()

    @Serializable
    val annotationDefaultTarget: TypeWithEntries<@Contextual AnnotationDefaultTargetMode> =
        AnnotationDefaultTargetModeType::class with AnnotationDefaultTargetMode.entries.toSet()

    @Serializable
    val nameBasedDestructuring: TypeWithEntries<@Contextual NameBasedDestructuringMode> =
        NameBasedDestructuringModeType::class with NameBasedDestructuringMode.entries.toSet()

    @Serializable
    val verifyIrMode: TypeWithEntries<@Contextual VerifyIrMode> =
        VerifyIrModeType::class with VerifyIrMode.entries.toSet()

    @Serializable
    val partialLinkageMode: TypeWithEntries<@Contextual PartialLinkageMode> =
        PartialLinkageModeType::class with PartialLinkageMode.entries.toSet()

    @Serializable
    val partialLinkageLogLevel: TypeWithEntries<@Contextual PartialLinkageLogLevel> =
        PartialLinkageLogLevelType::class with PartialLinkageLogLevel.entries.toSet()

    @Serializable
    val duplicatedUniqueNameStrategy: TypeWithEntries<@Contextual DuplicatedUniqueNameStrategy> =
        DuplicatedUniqueNameStrategyType::class with DuplicatedUniqueNameStrategy.entries.toSet()

    @Serializable
    val jsEcmaVersion: TypeWithEntries<@Contextual JsEcmaVersion> =
        JsEcmaVersionType::class with JsEcmaVersion.entries.toSet()

    @Serializable
    val jsModuleKind: TypeWithEntries<@Contextual JsModuleKind> =
        JsModuleKindType::class with JsModuleKind.entries.toSet()

    @Serializable
    val jsIrDiagnosticMode: TypeWithEntries<@Contextual JsIrDiagnosticMode> =
        JsIrDiagnosticModeType::class with JsIrDiagnosticMode.entries.toSet()

    @Serializable
    val jsMainCallMode: TypeWithEntries<@Contextual JsMainCallMode> =
        JsMainCallModeType::class with JsMainCallMode.entries.toSet()

    @Serializable
    val sourceMapEmbedSources: TypeWithEntries<@Contextual SourceMapEmbedSources> =
        SourceMapEmbedSourcesType::class with SourceMapEmbedSources.entries.toSet()

    @Serializable
    val sourceMapNamesPolicy: TypeWithEntries<@Contextual SourceMapNamesPolicy> =
        SourceMapNamesPolicyType::class with SourceMapNamesPolicy.entries.toSet()

    @Serializable
    val wasmTarget: TypeWithEntries<@Contextual WasmTarget> =
        WasmTargetType::class with WasmTarget.entries.toSet()

}

@Serializable
class TypeWithEntries<T>(
    val type: String,
    val values: Set<T>,
)

private infix fun <T> KClass<out EnumType<T>>.with(entries: Set<T>) where T : Enum<T>, T : WithStringRepresentation =
    TypeWithEntries(this.java.name, entries)

internal val allKotlinTypeSerializersModule = SerializersModule {
    contextualSerializerWithVersions<KotlinVersion>()
    contextualSerializerWithVersions<JvmTarget>()
    contextualSerializerWithVersions<ExplicitApiMode>()
    contextualSerializerWithVersions<ReturnValueCheckerMode>()
    contextualSerializerWithVersions<KlibIrInlinerMode>()
    contextualSerializerWithVersions<JvmDefaultMode>()
    contextualSerializerWithVersions<AbiStabilityMode>()
    contextualSerializerWithVersions<AssertionsMode>()
    contextualSerializerWithVersions<JspecifyAnnotationsMode>()
    contextualSerializerWithVersions<LambdasMode>()
    contextualSerializerWithVersions<SamConversionsMode>()
    contextualSerializerWithVersions<StringConcatMode>()
    contextualSerializerWithVersions<CompatqualAnnotationsMode>()
    contextualSerializerWithVersions<WhenExpressionsMode>()
    contextualSerializerWithVersions<JdkRelease>()
    contextualSerializerWithVersions<AnnotationDefaultTargetMode>()
    contextualSerializerWithVersions<NameBasedDestructuringMode>()
    contextualSerializerWithVersions<VerifyIrMode>()
    contextualSerializerWithVersions<PartialLinkageMode>()
    contextualSerializerWithVersions<PartialLinkageLogLevel>()
    contextualSerializerWithVersions<DuplicatedUniqueNameStrategy>()
    contextualSerializerWithVersions<JsEcmaVersion>()
    contextualSerializerWithVersions<JsModuleKind>()
    contextualSerializerWithVersions<JsIrDiagnosticMode>()
    contextualSerializerWithVersions<JsMainCallMode>()
    contextualSerializerWithVersions<SourceMapEmbedSources>()
    contextualSerializerWithVersions<SourceMapNamesPolicy>()
    contextualSerializerWithVersions<WasmTarget>()
}

private inline fun <reified T> SerializersModuleBuilder.contextualSerializerWithVersions() where T : WithKotlinReleaseVersionsMetadata, T : Enum<T>, T : WithStringRepresentation {
    contextual(allNamedTypeSerializerWithVersions<T>())
}
