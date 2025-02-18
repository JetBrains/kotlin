/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Note that the project created by this class is a completely non-functional stub.
 * It is only useful in tests that don't depend on Project's functionality.
 */
abstract class TestWithMockProject : TestWithDisposable() {
    private var _project: Project? = null
    protected val project: Project get() = _project!!

    @BeforeEach
    fun initProject() {
        _project = MockProject(null, disposable)
    }

    @AfterEach
    fun cleanup() {
        _project = null
    }
}
