/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object AnalysisFlags {
    @JvmStatic
    val skipMetadataVersionCheck by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val multiPlatformDoNotCheckActual by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val allowKotlinPackage by AnalysisFlag.Delegates.Boolean

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
}
