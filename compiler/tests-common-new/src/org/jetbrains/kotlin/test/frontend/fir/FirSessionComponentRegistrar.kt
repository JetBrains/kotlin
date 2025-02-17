/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

abstract class FirSessionComponentRegistrar : TestService {
    abstract fun registerAdditionalComponent(session: FirSession)
}

val TestServices.firSessionComponentRegistrar: FirSessionComponentRegistrar? by TestServices.nullableTestServiceAccessor()