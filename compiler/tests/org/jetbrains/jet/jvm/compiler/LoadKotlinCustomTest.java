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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.jet.test.util.NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaces;

/**
 * @author Pavel Talanov
 */
/*
 *  This test should be implemented via AbstractLoadCompiledKotlinTest.
 *  Atm it's not possible due to enums being loaded differently from binaries (in contrast to from sources).
 */
public final class LoadKotlinCustomTest extends TestCaseWithTmpdir {
    @NotNull
    private static final String PATH = "compiler/testData/loadKotlinCustom";
    @NotNull
    private static final String ENUM_DIR = PATH + "/enum";

    private void loadDescriptorsFromCompiledAndCompareWithTxt(@NotNull File expectedFile, @NotNull File kotlinFile)
            throws Exception {
        NamespaceDescriptor namespaceFromClass =
                compileKotlinAndLoadTestNamespaceDescriptorFromBinary(kotlinFile, tmpdir, myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        compareNamespaces(namespaceFromClass, namespaceFromClass, DONT_INCLUDE_METHODS_OF_OBJECT, expectedFile);
    }

    private void loadDescriptorsFromSourceAndCompareWithTxt(@NotNull File expectedFile, @NotNull File kotlinFile)
            throws Exception {
        AnalyzeExhaust exhaust = compileKotlinToDirAndGetAnalyzeExhaust(kotlinFile, tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY);
        NamespaceDescriptor namespaceFromSource = exhaust.getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR,
                                                                                  TEST_PACKAGE_FQNAME);
        assert namespaceFromSource != null;
        compareNamespaces(namespaceFromSource, namespaceFromSource, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.checkPrimaryConstructors(true),
                          expectedFile);
    }

    private void doTest(
            @NotNull String expectedSourceDescriptors,
            @NotNull String expectedBinaryDescriptors,
            @NotNull String kotlinFileName
    ) throws Exception {
        loadDescriptorsFromSourceAndCompareWithTxt(new File(expectedSourceDescriptors), new File(kotlinFileName));
        loadDescriptorsFromCompiledAndCompareWithTxt(new File(expectedBinaryDescriptors), new File(kotlinFileName));
    }

    private void doTest(@NotNull String dir) throws Exception {
        String name = getTestName(true);
        doTest(dir + "/" + name + "Source.txt",
               dir + "/" + name + "Binary.txt",
               dir + "/" + name + ".kt");
    }

    public void testSimpleEnum() throws Exception {
        doTest(ENUM_DIR);
    }

    public void testEnumVisibility() throws Exception {
        doTest(ENUM_DIR);
    }

    public void testInnerEnum() throws Exception {
        doTest(ENUM_DIR);
    }

    public void testInnerEnumExistingClassObject() throws Exception {
        doTest(ENUM_DIR);
    }
}
