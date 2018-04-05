/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

abstract class AbstractFirResolveWithSessionTestCase : KotlinTestWithEnvironment() {

    open fun createSession(): FirSession = FirJavaModuleBasedSession(FirTestModuleInfo(), project)
}