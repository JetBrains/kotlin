/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer

fun <T> runWriteAction(action: () -> T): T {
    return ApplicationManager.getApplication().runWriteAction<T>(action)
}

fun disposeRootDisposable(rootDisposable: Disposable) {
    val application: Application? = ApplicationManager.getApplication()
    // If core environment was not initialized, then the application wouldn't be available
    if (application != null) {
        application.runWriteAction {
            Disposer.dispose(rootDisposable)
        }
    } else {
        Disposer.dispose(rootDisposable)
    }
}

inline fun runWithDisposable(block: (Disposable) -> Unit) {
    val disposable = Disposer.newDisposable()
    try {
        block(disposable)
    } finally {
        disposeRootDisposable(disposable)
    }
}
