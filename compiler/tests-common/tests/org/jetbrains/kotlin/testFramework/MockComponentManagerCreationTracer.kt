/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.testFramework

import com.intellij.mock.MockComponentManager
import com.intellij.openapi.application.Application
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

object MockComponentManagerCreationTracer {

    private val creationTraceMap = ContainerUtil.createConcurrentWeakMap<MockComponentManager, Throwable>()

    @JvmStatic
    fun onCreate(manager: MockComponentManager) {
        creationTraceMap[manager] = Exception("Creation trace")
    }

    @JvmStatic
    fun onGetComponentInstance(manager: MockComponentManager) {
        if (manager.isDisposed) {
            val trace = creationTraceMap[manager] ?: return
            trace.printStackTrace(System.err)
        }
    }

    @JvmStatic
    fun diagnoseDisposedButNotClearedApplication(app: Application) {
        if (app is MockComponentManager) {
            KtUsefulTestCase.resetApplicationToNull()
            throw IllegalStateException("Some test disposed, but forgot to clear MockApplication", creationTraceMap[app])
        }
    }
}