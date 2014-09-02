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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.errorTypesAllowed;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;

public abstract class AbstractLazyResolveRecursiveComparingTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    protected void doTest(String testFileName) throws IOException {
        doTest(testFileName, true, true, true);
    }

    private void doTest(String testFileName, boolean checkPrimaryConstructors, boolean checkPropertyAccessors, boolean allowErrorTypes) throws IOException {
        List<JetFile> files = JetTestUtils.createTestFiles(
                testFileName,
                FileUtil.loadFile(new File(testFileName), true),
                new JetTestUtils.TestFileFactoryNoModules<JetFile>() {
                    @NotNull
                    @Override
                    public JetFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                        return JetPsiFactory(getProject()).createFile(fileName, text);
                    }
                }
        );

        ModuleDescriptor module = LazyResolveTestUtil.resolve(files, getEnvironment());

        String testedPackage = "test";
        PackageViewDescriptor testPackage = module.getPackage(new FqName(testedPackage));
        Assert.assertNotNull("Package for name '" + testedPackage + "' is null after resolve", testPackage);

        File serializeResultsTo = new File(FileUtil.getNameWithoutExtension(testFileName) + ".txt");

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                testPackage, RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                        .filterRecursion(RecursiveDescriptorComparator.SKIP_BUILT_INS_PACKAGES)
                        .checkPrimaryConstructors(checkPrimaryConstructors)
                        .checkPropertyAccessors(checkPropertyAccessors)
                        .withValidationStrategy(allowErrorTypes ? errorTypesAllowed() : errorTypesForbidden()),
                serializeResultsTo);
    }
}
