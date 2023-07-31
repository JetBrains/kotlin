/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

@RequiresOptIn("Internal Analysis API component which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
public annotation class KtAnalysisApiInternals

@RequiresOptIn("Internal Analysis API component which is used from other modules in the Kotlin project and is not supposed to be used anywhere else since it has no compatibility guarantees")
public annotation class KtAnalysisNonPublicApi

@RequiresOptIn("Analysis should not be allowed to be ran from EDT thread, otherwise it may cause IDE freezes")
public annotation class KtAllowAnalysisOnEdt

@RequiresOptIn("Analysis should not be allowed to be ran from write action, otherwise it may cause IDE freezes and incorrect behavior in some cases")
public annotation class KtAllowAnalysisFromWriteAction

