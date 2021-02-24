/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.fir

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.visualizer.AbstractVisualizerTest

abstract class AbstractFirVisualizerTest : AbstractVisualizerTest() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<*>> = ::FirFrontendFacade
    override val handler: Constructor<FrontendOutputHandler<*>> = ::FirOutputHandler

}
