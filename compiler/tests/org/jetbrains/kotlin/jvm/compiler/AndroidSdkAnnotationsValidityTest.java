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

package org.jetbrains.kotlin.jvm.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AndroidSdkAnnotationsValidityTest extends AbstractSdkAnnotationsValidityTest {

    @Override
    protected KotlinCoreEnvironment createEnvironment(Disposable parentDisposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.ANDROID_API, JetTestUtils.getAnnotationsJar());
        return KotlinCoreEnvironment.createForTests(parentDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @Override
    protected List<FqName> getClassesToValidate() throws IOException {
        JarFile jar = new JarFile(JetTestUtils.findAndroidApiJar());
        try {
            Enumeration<JarEntry> entries = jar.entries();
            Set<FqName> result = Sets.newLinkedHashSet();
            while (entries.hasMoreElements()){
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entry.isDirectory() && entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - ".class".length()).replace("/", ".").replace("$", ".");
                    result.add(new FqName(className));
                }
            }
            return Lists.newArrayList(result);
        } finally {
            jar.close();
        }
    }

}
