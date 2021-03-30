/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Field

class ApplicationEnvironmentDisposer : TestExecutionListener {
    companion object {
        val ROOT_DISPOSABLE: Disposable = Disposer.newDisposable()
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        KotlinCoreEnvironment.disposeApplicationEnvironment()
        Disposer.dispose(ROOT_DISPOSABLE)
        val ourApplicationField: Field = ApplicationManager::class.java.getDeclaredField("ourApplication")
        ourApplicationField.isAccessible = true
        ourApplicationField.set(null, null)
    }
}
