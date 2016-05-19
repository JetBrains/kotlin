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

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

interface ProjectEx : Project {
    fun init()

    fun setProjectName(name: String)
}

class MockProjectEx(parentDisposable: Disposable) : MockProject(if (ApplicationManager.getApplication() != null) ApplicationManager.getApplication().picoContainer else null, parentDisposable), ProjectEx {
    override fun setProjectName(name: String) {
    }

    override fun init() {
    }
}