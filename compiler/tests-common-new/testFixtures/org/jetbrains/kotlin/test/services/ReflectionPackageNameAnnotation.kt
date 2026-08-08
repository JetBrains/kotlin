/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives

/**
 * The `kotlin.internal.ReflectionPackageName` file annotation, which makes the compiler use the given
 * package name in the reflective information of the declarations of the annotated file, instead of the real one.
 *
 * [BatchingPackageInserter] adds this annotation to every file whose package it patches, so that the reflective
 * information (`KClass.qualifiedName`, `KClass.toString()`, the default `Any.toString()`, etc.) is not affected
 * by the package renaming, and thus is the same in grouping and non-grouping modes.
 *
 * Not every backend takes the annotation into account (see `IrFile.reflectionPackageName`), so this service acts as
 * an opt-in switch: test runners of the platforms supporting the annotation are expected to register it in
 * [TestServices] via `useAdditionalService { ReflectionPackageNameAnnotation }`, or, if they don't use [TestServices],
 * to pass it to `BatchingPackageInserter.PackageNamePatcher` directly.
 */
object ReflectionPackageNameAnnotation : TestService {
    val fqName: String = StandardClassIds.Annotations.ReflectionPackageName.asFqNameString()

    /** The opt-in markers required to use the annotation. */
    val requiredOptInMarkers: List<String> = listOf("kotlin.internal.InternalForKotlinTests")

    fun render(packageName: String): String = "$fqName(${packageName.quoteAsKotlinStringLiteral()})"

    /** Matches [render] with any argument, together with the `@file:` use-site target and the trailing line break. */
    val fileAnnotationRegex: Regex = Regex("@file:${Regex.escape(fqName)}\\(.*\\)\n")
}

val TestServices.reflectionPackageNameAnnotation: ReflectionPackageNameAnnotation? by TestServices.nullableTestServiceAccessor()

/**
 * Opts in to the markers required by the registered [ReflectionPackageNameAnnotation], if any.
 *
 * Note that the opt-ins are provided as an analysis flag rather than as the `OPT_IN` directive on purpose:
 * grouping test isolators may compute a batch token out of the language settings of a test
 * (see e.g. `WasmGroupingTestIsolator`), so an extra opt-in directive would affect grouping of all tests.
 */
class ReflectionPackageNameOptInConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> {
        val requiredOptInMarkers = testServices.reflectionPackageNameAnnotation?.requiredOptInMarkers.orEmpty()
        if (requiredOptInMarkers.isEmpty()) return emptyMap()
        return mapOf(AnalysisFlags.optIn to directives[LanguageSettingsDirectives.OPT_IN] + requiredOptInMarkers)
    }
}
