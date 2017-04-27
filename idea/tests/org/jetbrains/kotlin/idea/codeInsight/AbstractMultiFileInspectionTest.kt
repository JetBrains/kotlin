/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File

abstract class AbstractMultiFileInspectionTest : KotlinMultiFileTestCase() {
    init {
        myDoCompare = false
    }

    protected fun doTest(path: String) {
        val configFile = File(path)
        val config = JsonParser().parse(FileUtil.loadFile(configFile, true)) as JsonObject

        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        val withFullJdk = config["withFullJdk"]?.asBoolean ?: false
        isMultiModule = config["isMultiModule"]?.asBoolean ?: false

        doTest({ _, _ ->
                   try {
                       if (withRuntime) {
                           project.allModules().forEach { module ->
                               ConfigLibraryUtil.configureKotlinRuntimeAndSdk(
                                       module,
                                       if (withFullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
                               )
                           }
                       }

                       runInspection(Class.forName(config.getString("inspectionClass")), project,
                                     withTestDir = configFile.parent)
                   }
                   finally {
                       if (withRuntime) {
                           project.allModules().forEach { module ->
                               ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(
                                       module,
                                       if (withFullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
                               )
                           }
                       }
                   }
               },
               getTestDirName(true))
    }

    override fun getTestRoot() : String {
        return "/multiFileInspections/"
    }

    override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}
