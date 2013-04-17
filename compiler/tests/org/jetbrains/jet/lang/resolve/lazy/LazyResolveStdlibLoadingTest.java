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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.KotlinTestWithEnvironmentManagement;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
            NamespaceDescriptor eager = module.getNamespace(FqName.topLevel(name));
            NamespaceDescriptor lazy = lazyModule.getNamespace(FqName.topLevel(name));
            NamespaceComparator.compareNamespaces(eager, lazy, NamespaceComparator.RECURSIVE, null);
        }
    }

    public void testStdLib() throws Exception {
        doTestForGivenFiles(
                convertToJetFiles(collectKtFiles(STD_LIB_SRC))
        );
    }

    private List<JetFile> convertToJetFiles(List<File> files) throws IOException {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (File file : files) {
            JetFile jetFile = JetPsiFactory.createFile(stdlibEnvironment.getProject(), file.getName(), FileUtil.loadFile(file, true));
            jetFiles.add(jetFile);
        }
        return jetFiles;
    }

    private static List<File> collectKtFiles(@NotNull File root) {
        List<File> files = Lists.newArrayList();
        FileUtil.collectMatchedFiles(root, Pattern.compile(".*?.kt"), files);
        return files;
    }
}
