/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.model.BackendKind

abstract class BackendKindExtractor(protected val testServices: TestServices) : TestService {
    abstract fun backendKind(targetBackend: TargetBackend?): BackendKind<*>
}

val TestServices.backendKindExtractor: BackendKindExtractor by TestServices.testServiceAccessor()
