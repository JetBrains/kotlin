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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractLazyResolveNamespaceComparingTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    private void doTest(
            String testFileName
    ) throws IOException {
        List<JetFile> files = JetTestUtils
                .createTestFiles(testFileName, FileUtil.loadFile(new File(testFileName), true),
                                 new JetTestUtils.TestFileFactory<JetFile>() {
                                     @Override
                                     public JetFile create(String fileName, String text) {
                                         return JetPsiFactory.createFile(getProject(), fileName, text);
                                     }
                                 });

        ModuleDescriptor eagerModule = LazyResolveTestUtil.resolveEagerly(files, getEnvironment());
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(files, getEnvironment());

        Name test = Name.identifier("test");
        NamespaceDescriptor actual = lazyModule.getRootNamespace().getMemberScope().getNamespace(test);
        NamespaceDescriptor expected = eagerModule.getRootNamespace().getMemberScope().getNamespace(test);

        File serializeResultsTo = new File(FileUtil.getNameWithoutExtension(testFileName) + ".txt");

        NamespaceComparator.compareNamespaces(expected, actual,
                                              NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.filterOutput(new Predicate<NamespaceDescriptor>() {
                                                  @Override
                                                  public boolean apply(NamespaceDescriptor namespaceDescriptor) {
                                                      return !namespaceDescriptor.getName().equals(Name.identifier("jet"));
                                                  }
                                              }), serializeResultsTo);
    }

    protected void doTestSinglePackage(String testFileName) throws Exception {
        doTest(testFileName);
    }

    public static void main(String[] args) throws IOException {
        String extension = "kt";
        new TestGenerator(
            "compiler/tests/",
            AbstractLazyResolveNamespaceComparingTest.class.getPackage().getName(),
            "LazyResolveNamespaceComparingTestGenerated",
            AbstractLazyResolveNamespaceComparingTest.class,
            Arrays.asList(
                    new SimpleTestClassModel(new File("compiler/testData/readKotlinBinaryClass"),
                                             true,
                                             extension,
                                             "doTestSinglePackage"),
                    new SimpleTestClassModel(new File("compiler/testData/readJavaBinaryClass"),
                                             true,
                                             extension,
                                             "doTestSinglePackage"),
                    new SimpleTestClassModel(new File("compiler/testData/lazyResolve/namespaceComparator"),
                                             true,
                                             extension,
                                             "doTestSinglePackage")
            ),
            AbstractLazyResolveNamespaceComparingTest.class
        ).generateAndSave();
    }
}
