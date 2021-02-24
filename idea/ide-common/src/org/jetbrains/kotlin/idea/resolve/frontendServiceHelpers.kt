/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

/**
 * Helper methods for commonly used frontend components.
 * Use them to avoid explicit opt-ins.
 * Before adding a new helper method please make sure component doesn't have fragile invariants that can be violated by external use.
 */

@OptIn(FrontendInternals::class)
fun ResolutionFacade.getLanguageVersionSettings(): LanguageVersionSettings =
    frontendService<LanguageVersionSettings>()

@OptIn(FrontendInternals::class)
fun ResolutionFacade.getDataFlowValueFactory(): DataFlowValueFactory =
    frontendService<DataFlowValueFactory>()
