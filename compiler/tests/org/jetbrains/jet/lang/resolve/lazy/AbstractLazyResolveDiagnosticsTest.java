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
import junit.framework.TestCase;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.checkers.AbstractJetDiagnosticsTest;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.test.util.NamespaceComparator;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractLazyResolveDiagnosticsTest extends AbstractJetDiagnosticsTest {

    private static final File TEST_DATA_DIR = new File("compiler/testData/diagnostics/tests");

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    @Override
    protected void analyzeAndCheck(File testDataFile, String expectedText, List<TestFile> files) {
        List<JetFile> jetFiles = getJetFiles(files);
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(jetFiles, getEnvironment());
        ModuleDescriptor eagerModule = LazyResolveTestUtil.resolveEagerly(jetFiles, getEnvironment());

        String path = JetTestUtils.getFilePath(new File(FileUtil.getRelativePath(TEST_DATA_DIR, testDataFile)));
        String txtFileRelativePath = path.replaceAll("\\.kt$|\\.ktscript", ".txt");
        File txtFile = new File("compiler/testData/lazyResolve/diagnostics/" + txtFileRelativePath);
        NamespaceComparator.compareNamespaces(eagerModule.getRootNamespace(), lazyModule.getRootNamespace(), false,
                                              new Predicate<NamespaceDescriptor>() {
                                                  @Override
                                                  public boolean apply(NamespaceDescriptor descriptor) {
                                                      return !Name.identifier("jet").equals(descriptor.getName());
                                                  }
                                              },
                                              txtFile);
    }

    public static void main(String[] args) throws IOException {
        Class<? extends TestCase> thisClass = AbstractLazyResolveDiagnosticsTest.class;
        new TestGenerator(
                "compiler/tests/",
                thisClass.getPackage().getName(),
                "LazyResolveDiagnosticsTestGenerated",
                thisClass,
                Arrays.asList(
                        new SimpleTestClassModel(TEST_DATA_DIR, true, "kt", "doTest"),
                        new SimpleTestClassModel(new File("compiler/testData/diagnostics/tests/script"), true, "ktscript", "doTest")
                ),
                thisClass
        ).generateAndSave();
    }


}
