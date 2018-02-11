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

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.mock.MockProject
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

object JavacWrapperRegistrar {

    private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

    fun registerJavac(
            project: MockProject,
            configuration: CompilerConfiguration,
            javaFiles: List<File>,
            kotlinFiles: List<KtFile>,
            arguments: Array<String>?,
            bootClasspath: List<File>?,
            sourcePath: List<File>?,
            lightClassGenerationSupport: LightClassGenerationSupport
    ): Boolean {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        try {
            Class.forName(JAVAC_CONTEXT_CLASS)
        }
        catch (e: ClassNotFoundException) {
            messageCollector.report(ERROR, "'$JAVAC_CONTEXT_CLASS' class can't be found ('tools.jar' is not found)")
            return false
        }

        val context = Context()
        JavacLogger.preRegister(context, messageCollector)

        val jvmClasspathRoots = configuration.jvmClasspathRoots
        val outputDirectory = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        val compileJava = configuration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)
        val kotlinSupertypesResolver = JavacWrapperKotlinResolverImpl(lightClassGenerationSupport)

        val javacWrapper = JavacWrapper(javaFiles, kotlinFiles, arguments, jvmClasspathRoots, bootClasspath, sourcePath,
                                        kotlinSupertypesResolver, compileJava, outputDirectory, context)

        project.registerService(JavacWrapper::class.java, javacWrapper)

        return true
    }
}
