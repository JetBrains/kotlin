/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.NonGroupingStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilderBase
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ReflectionPackageNameAnnotation
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import java.io.File

/** The stdlib version that introduced `kotlin.internal.ReflectionPackageName`. */
val REFLECTION_PACKAGE_NAME_SINCE: LanguageVersion = LanguageVersion.KOTLIN_2_5

/**
 * Registers [ReflectionPackageNameAnnotation] only when the old stdlib has the annotation.
 *
 * A KLIB-compatibility test compiles its first stage against the stdlib of the old custom compiler version.
 * Annotation `kotlin.internal.ReflectionPackageName` exists in the stdlib only since Kotlin 2.5.
 * Registering it against an older stdlib would make every batched test fail its first stage with an unresolved reference,
 * which the suppressor then would silently mute, similar to not yet supported language feature.
 *
 * @param firstStageStdlibVersion the language version of the compiler whose stdlib is used on the first stage
 */
fun TestConfigurationBuilderBase<*, *>.useReflectionPackageNameAnnotationIfSupported(firstStageStdlibVersion: LanguageVersion) {
    if (firstStageStdlibVersion >= REFLECTION_PACKAGE_NAME_SINCE) {
        useAdditionalService { ReflectionPackageNameAnnotation }
    }
}

/**
 * Supplies `kotlin.internal.ReflectionPackageName` to a first stage whose stdlib predates it.
 *
 * A KLIB backward-compatibility test compiles its first stage against the stdlib of an old released compiler. When that
 * stdlib is older than [REFLECTION_PACKAGE_NAME_SINCE], the annotation class `BatchingPackageInserter` references is
 * supplied to that compiler as an additional source, moved into a dedicated helper module which the second stage never
 * links (see [getTransitivesAndFriendsWithoutReflectionPackageNameHelper]).
 *
 * @param firstStageStdlibVersion the language version of the compiler whose stdlib is used on the first stage
 */
@OptIn(TestInfrastructureInternals::class)
fun NonGroupingStageTestConfigurationBuilder.useReflectionPackageNameHelperIfNeeded(firstStageStdlibVersion: LanguageVersion) {
    if (firstStageStdlibVersion < REFLECTION_PACKAGE_NAME_SINCE) {
        useAdditionalSourceProviders(::ReflectionPackageNameAdditionalSourceProvider)
        useModuleStructureTransformers(ReflectionPackageNameHelperModuleTransformer)
    }
}

/**
 * Isolates the tests whose outcome depends on reflective package names when the second stage cannot keep them.
 *
 * A KLIB forward-compatibility test links its second stage with an old released compiler. When that compiler is older
 * than [REFLECTION_PACKAGE_NAME_SINCE], it does not know `kotlin.internal.ReflectionPackageName`, so a renamed test
 * cannot keep its reflective package names: such tests must not be renamed at all
 * (see [ReflectionPackageNameDependentTestIsolator]).
 *
 * @param secondStageCompilerVersion the language version of the compiler that links the second stage
 */
fun NonGroupingStageTestConfigurationBuilder.isolateReflectionPackageNameDependentTestsIfNeeded(
    secondStageCompilerVersion: LanguageVersion,
) {
    if (secondStageCompilerVersion < REFLECTION_PACKAGE_NAME_SINCE) {
        useGroupingTestIsolators(::ReflectionPackageNameDependentTestIsolator)
    }
}

/**
 * The regular and friend dependency KLIBs of this module, without the KLIB of the
 * [ReflectionPackageNameHelperModuleTransformer] helper module.
 *
 * That helper only lets a first-stage compiler older than 2.5 resolve `kotlin.internal.ReflectionPackageName`;
 * the current stdlib linked on the second stage already has the class, and linking the helper as well would
 * declare it twice (and, in a batch, several helper KLIBs would share one `unique_name`). The annotation
 * reference in the per-test KLIBs is then resolved by signature against the stdlib.
 */
fun TestModule.getTransitivesAndFriendsWithoutReflectionPackageNameHelper(testServices: TestServices): Pair<List<File>, List<File>> {
    val [transitiveLibraries: List<File>, friendLibraries: List<File>] = getTransitivesAndFriends(module = this, testServices)
    val helperKlibPath = testServices.klibEnvironmentConfigurator
        .getKlibArtifactFile(testServices, ReflectionPackageNameHelperModuleTransformer.HELPERS_MODULE_NAME)
        .absolutePath
    return transitiveLibraries.filterNot { it.absolutePath == helperKlibPath } to friendLibraries
}
