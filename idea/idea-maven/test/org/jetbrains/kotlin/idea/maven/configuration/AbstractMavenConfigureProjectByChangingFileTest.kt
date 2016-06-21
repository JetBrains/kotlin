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

package org.jetbrains.kotlin.idea.maven.configuration

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.kotlin.idea.configuration.AbstractConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.configuration.NotificationMessageCollector

abstract class AbstractMavenConfigureProjectByChangingFileTest : AbstractConfigureProjectByChangingFileTest<KotlinMavenConfigurator>() {
    fun doTestWithMaven(path: String) {
        val pathWithFile = path + "/" + MavenConstants.POM_XML
        doTest(pathWithFile, pathWithFile.replace("pom", "pom_after"), KotlinJavaMavenConfigurator())
    }

    fun doTestWithJSMaven(path: String) {
        val pathWithFile = path + "/" + MavenConstants.POM_XML
        doTest(pathWithFile, pathWithFile.replace("pom", "pom_after"), KotlinJavascriptMavenConfigurator())
    }

    override fun runConfigurator(module: Module, file: PsiFile, configurator: KotlinMavenConfigurator, version: String, collector: NotificationMessageCollector) {
        WriteCommandAction.runWriteCommandAction(module.project) {
            configurator.changePomFile(module, file, version, collector)
        }
    }
}
