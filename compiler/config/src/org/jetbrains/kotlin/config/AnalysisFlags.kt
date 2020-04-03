/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object AnalysisFlags {
    @JvmStatic
    val skipMetadataVersionCheck by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val multiPlatformDoNotCheckActual by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val klibBasedMpp by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val experimental by AnalysisFlag.Delegates.ListOfStrings

    @JvmStatic
    val useExperimental by AnalysisFlag.Delegates.ListOfStrings

    @JvmStatic
    val explicitApiVersion by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val ignoreDataFlowInAssert by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val allowResultReturnType by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val explicitApiMode by AnalysisFlag.Delegates.ApiModeDisabledByDefault

    @JvmStatic
    val constraintSystemForOverloadResolution by AnalysisFlag.Delegates.ConstraintSystemForOverloadResolution

    @JvmStatic
    val useTypeRefinement by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val ideMode by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val reportErrorsOnIrDependencies by AnalysisFlag.Delegates.Boolean
}
