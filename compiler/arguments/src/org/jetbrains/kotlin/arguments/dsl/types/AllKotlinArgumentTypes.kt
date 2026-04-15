/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi

/**
 * Class containing all non-primitive compiler argument types which are serialized in more detailed form.
 */
@OptIn(ExperimentalArgumentApi::class)
@Suppress("unused")
@Serializable
class AllKotlinArgumentTypes {
    val kotlinVersions = TypeWithEntries<KotlinVersionType, KotlinVersion>()
    val jvmTargets = TypeWithEntries<KotlinJvmTargetType, JvmTarget>()
    val explicitApiModes = TypeWithEntries<KotlinExplicitApiModeType, ExplicitApiMode>()
    val returnValueCheckerMode = TypeWithEntries<ReturnValueCheckerModeType, ReturnValueCheckerMode>()
    val klibIrInlinerMode = TypeWithEntries<KlibIrInlinerModeType, KlibIrInlinerMode>()
    val jvmDefaultMode = TypeWithEntries<JvmDefaultModeType, JvmDefaultMode>()
    val abiStabilityMode = TypeWithEntries<AbiStabilityModeType, AbiStabilityMode>()
    val assertionsMode = TypeWithEntries<AssertionsModeType, AssertionsMode>()
    val jspecifyAnnotationsMode = TypeWithEntries<JspecifyAnnotationsModeType, JspecifyAnnotationsMode>()
    val lambdasMode = TypeWithEntries<LambdasModeType, LambdasMode>()
    val samConversionsMode = TypeWithEntries<SamConversionsModeType, SamConversionsMode>()
    val stringConcatMode = TypeWithEntries<StringConcatModeType, StringConcatMode>()
    val compatqualAnnotationsMode = TypeWithEntries<CompatqualAnnotationsModeType, CompatqualAnnotationsMode>()
    val whenExpressionsMode = TypeWithEntries<WhenExpressionsModeType, WhenExpressionsMode>()
    val jdkRelease = TypeWithEntries<JdkReleaseType, JdkRelease>()
    val annotationDefaultTarget = TypeWithEntries<AnnotationDefaultTargetModeType, AnnotationDefaultTargetMode>()
    val nameBasedDestructuring = TypeWithEntries<NameBasedDestructuringModeType, NameBasedDestructuringMode>()
    val verifyIrMode = TypeWithEntries<VerifyIrModeType, VerifyIrMode>()
    val partialLinkageMode = TypeWithEntries<PartialLinkageModeType, PartialLinkageMode>()
    val partialLinkageLogLevel = TypeWithEntries<PartialLinkageLogLevelType, PartialLinkageLogLevel>()
    val duplicatedUniqueNameStrategy = TypeWithEntries<DuplicatedUniqueNameStrategyType, DuplicatedUniqueNameStrategy>()
    val jsEcmaVersion = TypeWithEntries<JsEcmaVersionType, JsEcmaVersion>()
    val jsModuleKind = TypeWithEntries<JsModuleKindType, JsModuleKind>()
    val jsIrDiagnosticMode = TypeWithEntries<JsIrDiagnosticModeType, JsIrDiagnosticMode>()
    val jsMainCallMode = TypeWithEntries<JsMainCallModeType, JsMainCallMode>()
    val sourceMapEmbedSources = TypeWithEntries<SourceMapEmbedSourcesType, SourceMapEmbedSources>()
    val sourceMapNamesPolicy = TypeWithEntries<SourceMapNamesPolicyType, SourceMapNamesPolicy>()
    val wasmTarget = TypeWithEntries<WasmTargetType, WasmTarget>()

}

@Serializable
class TypeWithEntries<T>(
    val type: String,
    val values: Set<T>,
)

private inline fun <reified S, reified T> TypeWithEntries() where T : Enum<T>, T : WithStringRepresentation =
    TypeWithEntries<@Contextual T>(S::class.java.name, T::class.java.enumConstants.toSet())
