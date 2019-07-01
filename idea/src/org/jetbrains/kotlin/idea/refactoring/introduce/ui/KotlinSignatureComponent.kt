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

package org.jetbrains.kotlin.idea.refactoring.introduce.ui

import com.intellij.openapi.project.Project
import com.intellij.refactoring.ui.MethodSignatureComponent
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinSignatureComponent(
        signature: String, project: Project
) : MethodSignatureComponent(signature, project, KotlinFileType.INSTANCE) {
    private val myFileName = "dummy." + KotlinFileType.EXTENSION

    override fun getFileName(): String = myFileName
}
