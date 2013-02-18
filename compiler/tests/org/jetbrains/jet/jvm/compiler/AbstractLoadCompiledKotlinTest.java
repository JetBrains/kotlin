/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetAnalyzeExhaust;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaces;

/**
 * Compile Kotlin and then parse model from .class files.
 */
@SuppressWarnings("JUnitTestCaseWithNoTests")
public abstract class AbstractLoadCompiledKotlinTest extends TestCaseWithTmpdir {
    public void doTest(@NotNull String ktFileName) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        AnalyzeExhaust exhaust = compileKotlinToDirAndGetAnalyzeExhaust(ktFile, tmpdir, getTestRootDisposable(),
                                                                        ConfigurationKind.JDK_ONLY);

        NamespaceDescriptor namespaceFromSource = exhaust.getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR,
                                                                                  TEST_PACKAGE_FQNAME);
        assert namespaceFromSource != null;
        Assert.assertEquals("test", namespaceFromSource.getName().getName());
        NamespaceDescriptor namespaceFromClass = LoadDescriptorUtil.loadTestNamespaceAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY).first;
        compareNamespaces(namespaceFromSource, namespaceFromClass,
                          NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.checkPrimaryConstructors(true), txtFile);
    }
}
