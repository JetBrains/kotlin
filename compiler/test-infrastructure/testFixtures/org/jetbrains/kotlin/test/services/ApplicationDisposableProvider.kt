/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.Disposable

abstract class ApplicationDisposableProvider : TestService {
    abstract fun getApplicationRootDisposable(): Disposable
}

val TestServices.applicationDisposableProvider: ApplicationDisposableProvider by TestServices.testServiceAccessor()
