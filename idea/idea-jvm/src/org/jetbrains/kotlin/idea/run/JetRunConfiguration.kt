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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.ExternalizablePath
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.configurations.RefactoringListenerProvider

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
@Deprecated("Will be dropped in 1.2.20. Use KotlinRunConfiguration instead.")
abstract class JetRunConfiguration(
    name: String,
    runConfigurationModule: JavaRunConfigurationModule,
    factory: ConfigurationFactory
) :
    ModuleBasedConfigurationElement<JavaRunConfigurationModule>(name, runConfigurationModule, factory),
    CommonJavaRunConfigurationParameters,
    RefactoringListenerProvider {

    @JvmField
    var MAIN_CLASS_NAME: String? = null

    @JvmField
    var WORKING_DIRECTORY: String? = null

    override fun setWorkingDirectory(value: String?) {
        WORKING_DIRECTORY = ExternalizablePath.urlValue(value)
    }

    override fun getWorkingDirectory(): String? {
        return ExternalizablePath.localPathValue(WORKING_DIRECTORY)
    }
}