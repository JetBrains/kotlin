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

import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.jvm.compiler.ExpectedLoadErrorsUtil;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.io.File;
import java.util.Arrays;

public class LoadBuiltinsTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    public void testBuiltIns() throws Exception {
        RecursiveDescriptorComparator.Configuration configuration = RecursiveDescriptorComparator.RECURSIVE_ALL.withRenderer(
                new DescriptorRendererBuilder()
                        .setWithDefinedIn(false)
                        .setExcludedAnnotationClasses(Arrays.asList(new FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)))
                        .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
                        .setVerbose(true)
                        .setAlwaysRenderAny(true)       // TODO is it needed?
                        .setPrettyFunctionTypes(false)  // TODO is it needed?
                        .build()
        );

        PackageFragmentDescriptor actualFragment = KotlinBuiltIns.getInstance().getBuiltInsPackageFragment();
        RecursiveDescriptorComparator
                .validateAndCompareDescriptorWithFile(actualFragment, configuration, new File("compiler/testData/builtin-classes.txt"));
    }
}
