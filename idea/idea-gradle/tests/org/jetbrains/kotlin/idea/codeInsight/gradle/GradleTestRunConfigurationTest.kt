/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import org.junit.Test
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.run.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions

class GradleTestRunConfigurationTest : GradleImportingTestCase() {
    @Test
    @TargetVersions("4.7+")
    fun testPreferredConfigurations() {
        if (!PlatformUtils.isIntelliJ()) {
            return
        }

        val files = importProjectFromTestData()

        runInEdtAndWait {
            runReadAction {
                val javaFile = files.first { it.name == "MyTest.java" }
                val kotlinFile = files.first { it.name == "MyKotlinTest.kt" }

                val javaClassConfiguration = getConfiguration(javaFile, myProject, "MyTest")
                javaClassConfiguration.isProducedBy(TestClassGradleConfigurationProducer::class.java)
                assert(javaClassConfiguration.configuration !is KotlinGradleRunConfiguration)

                val javaMethodConfiguration = getConfiguration(javaFile, myProject, "testA")
                javaMethodConfiguration.isProducedBy(TestMethodGradleConfigurationProducer::class.java)
                assert(javaMethodConfiguration.configuration !is KotlinGradleRunConfiguration)

                val kotlinClassConfiguration = getConfiguration(kotlinFile, myProject, "MyKotlinTest")
                kotlinClassConfiguration.isProducedBy(KotlinJvmTestClassGradleConfigurationProducer::class.java)
                assert(kotlinClassConfiguration.configuration is KotlinGradleRunConfiguration)

                val kotlinFunctionConfiguration = getConfiguration(kotlinFile, myProject, "testA")
                kotlinFunctionConfiguration.isProducedBy(KotlinJvmTestMethodGradleConfigurationProducer::class.java)
                assert(kotlinFunctionConfiguration.configuration is KotlinGradleRunConfiguration)
            }
        }
    }

    @Test
    @TargetVersions("4.7+")
    fun testKotlinJUnitSettings() {
        if (!PlatformUtils.isIntelliJ()) {
            return
        }

        importProjectFromTestData()

        runInEdtAndWait {
            runReadAction {
                fun getConfiguration(cls: Class<out ConfigurationType>): JUnitConfiguration {
                    val configurationFactory = ConfigurationTypeUtil
                        .findConfigurationType(cls).configurationFactories[0]
                    val runManager = RunManagerEx.getInstanceEx(myProject)
                    val kotlinRunnerAndConfigurationSettings = runManager.getConfigurationTemplate(configurationFactory)
                    return kotlinRunnerAndConfigurationSettings.configuration as JUnitConfiguration
                }

                val javaConfiguration = getConfiguration(JUnitConfigurationType::class.java)
                val kotlinConfiguration = getConfiguration(KotlinJUnitConfigurationType::class.java)

                assert(javaConfiguration !== kotlinConfiguration)
                assert(javaConfiguration.javaClass != kotlinConfiguration.javaClass)

                assertEquals("-Dfoo=bar", javaConfiguration.persistentData.vmParameters)
                assertEquals("-Dfoo=bar", kotlinConfiguration.persistentData.vmParameters)
            }
        }
    }

    override fun testDataDirName(): String {
        return "testRunConfigurations"
    }
}