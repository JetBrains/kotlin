/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

abstract class TestDisposableProvider : TestService {
    abstract val testServices: TestServices
    abstract fun registerDisposables(projectDisposable: Disposable, applicationDisposable: Disposable)

    abstract fun getProjectDisposable(): Disposable

    abstract fun getApplicationDisposable(): Disposable
}

class TestDisposableProviderImpl(override val testServices: TestServices) : TestDisposableProvider() {
    private lateinit var _projectDisposable: Disposable
    private lateinit var _applicationDisposable: Disposable

    override fun registerDisposables(projectDisposable: Disposable, applicationDisposable: Disposable) {
        require(!this::_projectDisposable.isInitialized)
        require(!this::_applicationDisposable.isInitialized)

        this._projectDisposable = projectDisposable
        this._applicationDisposable = applicationDisposable
    }

    override fun getProjectDisposable(): Disposable = _projectDisposable
    override fun getApplicationDisposable(): Disposable = _applicationDisposable
}

val TestServices.disposableProvider: TestDisposableProvider by TestServices.testServiceAccessor()