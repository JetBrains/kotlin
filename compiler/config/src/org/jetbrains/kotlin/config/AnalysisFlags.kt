/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object AnalysisFlags {
    @JvmStatic
    val skipMetadataVersionCheck by AnalysisFlag.Delegates.Boolean

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
    val allowFullyQualifiedNameInKClass by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val eagerResolveOfLightClasses by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val dontWarnOnErrorSuppression by AnalysisFlag.Delegates.Boolean
}
