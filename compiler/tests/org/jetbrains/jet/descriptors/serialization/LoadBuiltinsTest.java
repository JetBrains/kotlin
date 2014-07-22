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

package org.jetbrains.jet.descriptors.serialization;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class LoadBuiltinsTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    public void testBuiltIns() throws Exception {
        RecursiveDescriptorComparator.Configuration configuration =
                RecursiveDescriptorComparator.RECURSIVE_ALL.includeMethodsOfKotlinAny(false).withRenderer(
                        new DescriptorRendererBuilder()
                                .setWithDefinedIn(false)
                                .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
                                .setVerbose(true)
                                .setPrettyFunctionTypes(false)
                                .build()
                );

        List<JetFile> files = JetTestUtils.loadToJetFiles(getEnvironment(), ContainerUtil.concat(
                allFilesUnder("core/builtins/native"),
                allFilesUnder("core/builtins/src")
        ));

        PackageFragmentDescriptor deserialized = KotlinBuiltIns.getInstance().getBuiltInsPackageFragment();

        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(files, getEnvironment(), false);
        List<PackageFragmentDescriptor> fragments =
                module.getPackageFragmentProvider().getPackageFragments(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
        for (PackageFragmentDescriptor fromLazyResolve : fragments) {
            if (fromLazyResolve instanceof LazyPackageDescriptor) {
                RecursiveDescriptorComparator.validateAndCompareDescriptors(
                        fromLazyResolve, deserialized, configuration,
                        new File("compiler/testData/builtin-classes.txt")
                );
                break;
            }
        }
    }

    @NotNull
    private static List<File> allFilesUnder(@NotNull String directory) {
        return FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), new File(directory));
    }
}
