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

package org.jetbrains.jet.plugin.stubs;

import com.google.common.base.Predicate;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.Consumer;
import kotlin.Function0;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.KotlinCodeInsightTestCase;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheService;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;

import static com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.FORBID_ERROR_TYPES;

public abstract class AbstractLazyResolveByStubTest extends KotlinCodeInsightTestCase {
    protected void doTestCheckingPrimaryConstructorsAndAccessors(String testFileName) throws Exception {
        doTest(testFileName, true, true);
    }

    protected void doTestNotCheckingPrimaryConstructors(String testFileName) throws Exception {
        doTest(testFileName, false, false);
    }

    public void doTest(@NotNull final String path, final boolean checkPrimaryConstructors, final boolean checkPropertyAccessors)
            throws Exception {
        configureByFile(path);
        configureModule(getModule(), JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE);
        boolean shouldFail = getTestName(false).equals("ClassWithConstVal");
        AstAccessControl.INSTANCE$.testWithControlledAccessToAst(
                shouldFail, getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        performTest(path, checkPrimaryConstructors, checkPropertyAccessors);
                        return Unit.INSTANCE$;
                    }
                }
        );
    }

    private void performTest(@NotNull String path, boolean checkPrimaryConstructors, boolean checkPropertyAccessors) {
        ResolveSessionForBodies resolveSession =
                KotlinCacheService.OBJECT$.getInstance(getFile().getProject()).getLazyResolveSession((JetFile) getFile());
        ModuleDescriptor module = resolveSession.getModuleDescriptor();
        PackageViewDescriptor packageViewDescriptor = module.getPackage(new FqName("test"));
        Assert.assertNotNull(packageViewDescriptor);

        File fileToCompareTo = new File(FileUtil.getNameWithoutExtension(path) + ".txt");

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                packageViewDescriptor,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT.filterRecursion(
                        new Predicate<FqName>() {
                            @Override
                            public boolean apply(FqName fqName) {
                                return !KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName);
                            }
                        }
                )
                        .checkPrimaryConstructors(checkPrimaryConstructors)
                        .checkPropertyAccessors(checkPropertyAccessors)
                        .withValidationStrategy(FORBID_ERROR_TYPES),
                fileToCompareTo
        );
    }

    private static void configureModule(@NotNull final Module module, @NotNull final LightProjectDescriptor descriptor) {
        updateModel(module, new Consumer<ModifiableRootModel>() {
            @Override
            public void consume(ModifiableRootModel model) {
                if (descriptor.getSdk() != null) {
                    model.setSdk(descriptor.getSdk());
                }
                ContentEntry entry = model.getContentEntries()[0];
                descriptor.configureModule(module, model, entry);
            }
        });
    }

    @Override
    protected String getTestDataPath() {
        return "";
    }
}
