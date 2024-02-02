/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object AnalysisFlags {
    @JvmStatic
    val skipMetadataVersionCheck by AnalysisFlag.Delegates.Boolean

    /**
     * Metadata compilation is run for every non-platform fragment (for common and intermediate fragments).
     *
     * The initial purpose of the metadata compilation is to produce metadata artifacts for non-platform source sets.
     * These metadata artifacts are needed only for IDE.
     *
     * But metadata compilation is also used to report diagnostics when regular compilation can't do that for whatever reason (My opinion,
     * that the reason is broken design of `expect`/`actual` KT-64130).
     * E.g. `ABSTRACT_NOT_IMPLEMENTED` (KT-66205)
     *
     * This flag is true in two case:
     * 1. Metadata compilation (`:compile*KotlinMetadata` Gradle task)
     * 2. IDE analysis
     *
     * Known metadata compilation problems: KT-66382
     */
    @JvmStatic
    val metadataCompilation by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val skipPrereleaseCheck by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val multiPlatformDoNotCheckActual by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val skipExpectedActualDeclarationChecker by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val optIn by AnalysisFlag.Delegates.ListOfStrings

    @JvmStatic
    val explicitApiVersion by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val ignoreDataFlowInAssert by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val explicitApiMode by AnalysisFlag.Delegates.ApiModeDisabledByDefault

    @JvmStatic
    val explicitReturnTypes by AnalysisFlag.Delegates.ApiModeDisabledByDefault

    @JvmStatic
    val ideMode by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val allowUnstableDependencies by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val libraryToSourceAnalysis by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val extendedCompilerChecks by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val allowKotlinPackage by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val muteExpectActualClassesWarning by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val consistentDataClassCopyVisibility by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val allowFullyQualifiedNameInKClass by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val eagerResolveOfLightClasses by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val dontWarnOnErrorSuppression by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val stdlibCompilation by AnalysisFlag.Delegates.Boolean
}

fun LanguageVersionSettings.doesDataClassCopyRespectConstructorVisibility(): Boolean {
    return getFlag(AnalysisFlags.consistentDataClassCopyVisibility) ||
            supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility)
}
