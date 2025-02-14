/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

class SkipK1MetaTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean = testServices.defaultsProvider.frontendKind == FrontendKinds.ClassicFrontend
}
