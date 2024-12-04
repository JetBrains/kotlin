/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*

typealias TestFilter = (AnalysisApiTestConfiguratorFactoryData) -> Boolean

infix fun TestFilter.and(other: TestFilter): TestFilter =
    { data -> this(data) && other(data) }

infix fun TestFilter.or(other: TestFilter): TestFilter =
    { data -> this(data) || other(data) }

fun frontendIs(vararg frontends: FrontendKind): TestFilter =
    { it.frontend in frontends }

fun testModuleKindIs(vararg moduleKinds: TestModuleKind): TestFilter =
    { it.moduleKind in moduleKinds }

fun analysisSessionModeIs(vararg modes: AnalysisSessionMode): TestFilter =
    { it.analysisSessionMode in modes }

fun analysisApiModeIs(vararg modes: AnalysisApiMode): TestFilter =
    { it.analysisApiMode in modes }


