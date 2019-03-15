/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.inspections.runInspection
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
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
                   val sdk = if (withFullJdk) fullJdk() else mockJdk()
                   addJdk(testRootDisposable) { sdk }

                   try {
                       if (withRuntime) {
                           project.allModules().forEach { module ->
                               ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, sdk)
                           }
                       }

                       runInspection(Class.forName(config.getString("inspectionClass")), project,
                                     withTestDir = configFile.parent)
                   }
                   finally {
                       if (withRuntime) {
                           project.allModules().forEach { module ->
                               ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(module, sdk)
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
        return getTestDataPathBase()
    }
}
