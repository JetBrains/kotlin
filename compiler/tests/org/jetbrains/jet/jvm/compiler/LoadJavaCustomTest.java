/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileJavaAndLoadTestNamespaceFromBinary;

/**
 * @author Pavel Talanov
 */
/*
   LoadJavaTestGenerated should be used instead if possible.
 */
public final class LoadJavaCustomTest extends KotlinTestWithEnvironment {
    @NotNull
    private static final String PATH = "compiler/testData/loadJavaCustom";

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    private void doTest(@NotNull String expectedFileName, @NotNull String... javaFileNames) throws Exception {
        Collection<File> files = Collections2.transform(Arrays.asList(javaFileNames), new Function<String, File>() {
            @Override
            public File apply(String s) {
                return new File(s);
            }
        });
        File expected = new File(expectedFileName);
        File tmpDir = JetTestUtils.tmpDir(expected.getName());
        NamespaceDescriptor javaNamespaceDescriptor = compileJavaAndLoadTestNamespaceFromBinary(files, tmpDir, getTestRootDisposable());
        //NOTE: comparing namespace to file (hack)
        NamespaceComparator.compareNamespaces(javaNamespaceDescriptor, javaNamespaceDescriptor, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT, expected);
    }

    public void testPackageLocalVisibility() throws Exception {
        String dir = PATH + "/packageLocalVisibility";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/test/JFrame.java",
               javaDir + "/awt/Frame.java");
    }

    public void testInner() throws Exception {
        doSimpleTest();
    }

    public void testStaticFinal() throws Exception {
        String dir = "/staticFinal/";
        doTest(PATH + dir + "expected.txt",
               PATH + dir + "test.java");
    }

    private void doSimpleTest() throws Exception {
        doTest(PATH + "/" + getTestName(true) + ".txt",
               PATH + "/" + getTestName(true) + ".java");
    }

    //TODO: move to LoadJavaTestGenerated when possible
    public void testEnum() throws Exception {
        String dir = PATH + "/enum";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/MyEnum.java");
    }
}
