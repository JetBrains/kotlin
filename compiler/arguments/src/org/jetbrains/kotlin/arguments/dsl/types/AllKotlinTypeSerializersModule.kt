/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.contextual
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.base.allNamedTypeSerializerWithVersions

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
