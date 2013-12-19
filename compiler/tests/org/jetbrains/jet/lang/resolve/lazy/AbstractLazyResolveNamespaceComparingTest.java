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
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.FORBID_ERROR_TYPES;

public abstract class AbstractLazyResolveNamespaceComparingTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    protected void doTestCheckingPrimaryConstructors(String testFileName) throws IOException {
        doTest(testFileName, true, false, true);
    }

    protected void doTestCheckingPrimaryConstructorsAndAccessors(String testFileName) throws IOException {
        doTest(testFileName, true, true, false);
    }

    protected void doTestNotCheckingPrimaryConstructors(String testFileName) throws IOException {
        doTest(testFileName, false, false, false);
    }

    private void doTest(String testFileName, boolean checkPrimaryConstructors, boolean checkPropertyAccessors, boolean allowErrorTypes) throws IOException {
        List<JetFile> files = JetTestUtils
                .createTestFiles(testFileName, FileUtil.loadFile(new File(testFileName), true),
                                 new JetTestUtils.TestFileFactory<JetFile>() {
                                     @Override
                                     public JetFile create(String fileName, String text, Map<String, String> directives) {
                                         return JetPsiFactory.createFile(getProject(), fileName, text);
                                     }
                                 });

        ModuleDescriptor eagerModule = LazyResolveTestUtil.resolveEagerly(files, getEnvironment());
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(files, getEnvironment());

        FqName test = new FqName("test");

        PackageViewDescriptor actual = lazyModule.getPackage(test);
        Assert.assertNotNull("Namespace for name " + test + " is null after lazy resolve", actual);

        PackageViewDescriptor expected = eagerModule.getPackage(test);
        Assert.assertNotNull("Namespace for name " + test + " is null after eager resolve", expected);

        File serializeResultsTo = new File(FileUtil.getNameWithoutExtension(testFileName) + ".txt");

        RecursiveDescriptorComparator.validateAndCompareDescriptors(
                expected, actual, RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT.filterRecursion(
                new Predicate<FqName>() {
                    @Override
                    public boolean apply(FqName fqName) {
                        return !KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName);
                    }
                })
                .checkPrimaryConstructors(checkPrimaryConstructors)
                .checkPropertyAccessors(checkPropertyAccessors)
                .withValidationStrategy(allowErrorTypes ? ALLOW_ERROR_TYPES : FORBID_ERROR_TYPES),
                serializeResultsTo);
    }
}
