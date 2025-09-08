/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData

internal fun AnalysisApiTestGroup.component(
    directory: String,
    filter: (AnalysisApiTestConfiguratorFactoryData) -> Boolean = { true },
    init: AnalysisApiTestGroup.() -> Unit,
) {
    group("components/$directory", filter, init)
}


