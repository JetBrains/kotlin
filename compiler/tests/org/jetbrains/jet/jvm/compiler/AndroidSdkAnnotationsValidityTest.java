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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AndroidSdkAnnotationsValidityTest extends AbstractSdkAnnotationsValidityTest {

    @Override
    protected JetCoreEnvironment createEnvironment(Disposable parentDisposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.ANDROID_API, JetTestUtils.getAnnotationsJar());
        return JetCoreEnvironment.createForTests(parentDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
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
