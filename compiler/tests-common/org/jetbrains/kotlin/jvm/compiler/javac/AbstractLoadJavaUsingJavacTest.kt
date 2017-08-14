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

package org.jetbrains.kotlin.jvm.compiler.javac

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractLoadJavaUsingJavacTest : AbstractLoadJavaTest() {
    override fun registerJavacIfNeeded(environment: KotlinCoreEnvironment) {
        environment.registerJavac()
        environment.configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
    }

    override fun useJavacWrapper() = true

    override fun getExpectedFile(expectedFileName: String): File {
        val differentResultFile = KotlinTestUtils.replaceExtension(File(expectedFileName), "javac.txt")
        if (differentResultFile.exists()) return differentResultFile
        return super.getExpectedFile(expectedFileName)
    }

}

object JavacRegistrarForTests {
    fun registerJavac(environment: KotlinCoreEnvironment) {
        environment.registerJavac()
    }
}