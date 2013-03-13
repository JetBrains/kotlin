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
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AbstractLazyResolveNamespaceComparingTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    protected void doTestCheckingPrimaryConstructors(String testFileName) throws IOException {
        doTest(testFileName, true, false);
    }

    protected void doTestCheckingPrimaryConstructorsAndAccessors(String testFileName) throws IOException {
        doTest(testFileName, true, true);
    }

    protected void doTestNotCheckingPrimaryConstructors(String testFileName) throws IOException {
        doTest(testFileName, false, false);
    }

    private void doTest(String testFileName, boolean checkPrimaryConstructors, boolean checkPropertyAccessors) throws IOException {
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

        NamespaceComparator.compareNamespaces(
                expected, actual, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT.filterRecursion(
                new Predicate<FqNameUnsafe>() {
                    @Override
                    public boolean apply(FqNameUnsafe fqName) {
                        return !KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.toUnsafe().equals(fqName);
                    }
                }).checkPrimaryConstructors(checkPrimaryConstructors).checkPropertyAccessors(checkPropertyAccessors), serializeResultsTo);
    }
}
