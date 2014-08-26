/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.jet.ConfigurationKind
import com.intellij.compiler.CompilerConfiguration
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.JetTestUtils
import java.util.Collections
import com.intellij.util.ArrayUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.util.ArrayList
import com.intellij.util.Processor
import org.jetbrains.jet.codegen.CodegenTestFiles

public abstract class AbstractAndroidBoxTest : AbstractBlackBoxCodegenTest() {

    fun createEnvironment(path: String) {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API);
        configuration.put(JVMConfigurationKeys.ANDROID_RES_PATH, path + "layout/");
        configuration.put(JVMConfigurationKeys.ANDROID_MANIFEST, path + "AndroidManifest.xml");
        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration);
    }

    override fun doTest(path: String) {
        createEnvironment(path)
        val files = ArrayList<String>(2)
        FileUtil.processFilesRecursively(File(path), object : Processor<File> {
            override fun process(file: File?): Boolean {
                if (file!!.getName().endsWith(".kt")) {
                    files.add(relativePath(file))
                }
                return true
            }
        })
        Collections.sort(files);
        myFiles = CodegenTestFiles.create(myEnvironment!!.getProject(), ArrayUtil.toStringArray(files))
        blackBox();
    }

}
