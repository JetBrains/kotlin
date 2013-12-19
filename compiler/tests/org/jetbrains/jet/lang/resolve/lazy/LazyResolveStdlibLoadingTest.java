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

import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.KotlinTestWithEnvironmentManagement;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;

import java.io.File;
import java.util.List;
import java.util.Set;

public class LazyResolveStdlibLoadingTest extends KotlinTestWithEnvironmentManagement {

    private static final File STD_LIB_SRC = new File("libraries/stdlib/src");
    private JetCoreEnvironment stdlibEnvironment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        stdlibEnvironment = createEnvironmentWithJdk(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    protected void doTestForGivenFiles(
            List<JetFile> files
    ) {
        Set<Name> namespaceShortNames = LazyResolveTestUtil.getTopLevelPackagesFromFileList(files);

        ModuleDescriptor module = LazyResolveTestUtil.resolveEagerly(files, stdlibEnvironment);
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(files, stdlibEnvironment);

        for (Name name : namespaceShortNames) {
            PackageViewDescriptor eager = module.getPackage(FqName.topLevel(name));
            PackageViewDescriptor lazy = lazyModule.getPackage(FqName.topLevel(name));
            RecursiveDescriptorComparator.validateAndCompareDescriptors(eager, lazy, RecursiveDescriptorComparator.RECURSIVE, null);
        }
    }

    public void testStdLib() throws Exception {
        doTestForGivenFiles(
                JetTestUtils.loadToJetFiles(stdlibEnvironment, JetTestUtils.collectKtFiles(STD_LIB_SRC))
        );
    }
}
