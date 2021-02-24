/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.psi

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.visualizer.AbstractVisualizerTest

abstract class AbstractPsiVisualizerTest : AbstractVisualizerTest() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.ClassicFrontend
    override val frontendFacade: Constructor<FrontendFacade<*>> = ::ClassicFrontendFacade
    override val handler: Constructor<FrontendOutputHandler<*>> = ::PsiOutputHandler
}
