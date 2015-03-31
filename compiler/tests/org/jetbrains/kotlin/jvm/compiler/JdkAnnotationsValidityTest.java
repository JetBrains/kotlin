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
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdkAnnotationsValidityTest extends AbstractSdkAnnotationsValidityTest {

    // KT-4359 Alternative signature checking problem: Set<?> is incompatible with Set<Object>
    //
    // <item name='javax.management.openmbean.TabularDataSupport java.util.Set&lt;java.lang.Object&gt; keySet()'>
    //   <annotation name='org.jetbrains.annotations.NotNull'/>
    // </item>
    // <item name='java.util.Map java.util.Set&lt;K&gt; keySet()'>
    //   <annotation name='org.jetbrains.annotations.NotNull'/>
    //   <annotation name='jet.runtime.typeinfo.KotlinSignature'>
    //     <val name="value" val="&quot;fun keySet() : Set&lt;K&gt;&quot;"/>
    //   </annotation>
    // </item>
    // <item name='javax.management.openmbean.TabularData java.util.Set&lt;?&gt; keySet()'>
    //   <annotation name='org.jetbrains.annotations.NotNull'/>
    // </item>
    //
    // KAnnotator produces above annotations and validation of TabularDataSupport results into:
    // public open fun keySet(): kotlin.MutableSet<kotlin.Any> defined in javax.management.openmbean.TabularDataSupport :
    // [Incompatible types in superclasses: [Any?, Any, Any], Incompatible projection kinds in type arguments of super methods' return types: [out Any?, Any, Any]]
    private static final Set<String> classesToIgnore = new HashSet<String>(Arrays.asList("javax.management.openmbean.TabularDataSupport"));

    private static KotlinCoreEnvironment createFullJdkEnvironment(Disposable parentDisposable) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK, JetTestUtils.getAnnotationsJar());
        configuration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File("ideaSDK/lib/jdkAnnotations.jar"));
        return KotlinCoreEnvironment.createForTests(parentDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @Override
    protected KotlinCoreEnvironment createEnvironment(Disposable parentDisposable) {
        return createFullJdkEnvironment(parentDisposable);
    }

    @Override
    protected List<FqName> getClassesToValidate() throws IOException {
        return getAffectedClasses("jar://dependencies/annotations/kotlin-jdk-annotations.jar!/");
    }

    static List<FqName> getAffectedClasses(String rootUrl) {
        Disposable myDisposable = Disposer.newDisposable();

        try {
            createFullJdkEnvironment(myDisposable);

            VirtualFile root = VirtualFileManager.getInstance().findFileByUrl(rootUrl);
            assert root != null;

            final Set<FqName> result = Sets.newLinkedHashSet();
            VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (ExternalAnnotationsManager.ANNOTATIONS_XML.equals(file.getName())) {
                        try {
                            String text = StreamUtil.readText(file.getInputStream());
                            Matcher matcher = Pattern.compile("<item name=['\"]([\\w\\d\\.]+)[\\s'\"]").matcher(text);
                            while (matcher.find()) {
                                String className = matcher.group(1);
                                if (!classesToIgnore.contains(className)) {
                                    result.add(new FqName(className));
                                }
                            }
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return true;
                }
            });

            return Lists.newArrayList(result);
        }
        finally {
            Disposer.dispose(myDisposable);
        }

    }
}
