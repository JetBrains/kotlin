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

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription

object KotlinStdJSProjectDescriptor : KotlinLightProjectDescriptor() {
    override fun getModuleType(): ModuleType<*> = StdModuleTypes.JAVA
    override fun getSdk(): Sdk? = null

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        val configuration = JSLibraryStdDescription(module.project).createNewLibraryForTests()

        val editor = NewLibraryEditor(configuration.libraryType, configuration.properties)
        configuration.addRoots(editor)

        ConfigLibraryUtil.addLibrary(editor, model, JSLibraryKind)
    }
}