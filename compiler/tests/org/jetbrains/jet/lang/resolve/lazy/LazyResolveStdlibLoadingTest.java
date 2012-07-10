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
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.jvm.compiler.NamespaceComparator;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class LazyResolveStdlibLoadingTest extends AbstractLazyResolveTest {

    private static final File STD_LIB_SRC = new File("libraries/stdlib/src");
    private JetCoreEnvironmentWithDisposable stdlibEnvironment;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        stdlibEnvironment = new JetCoreEnvironmentWithDisposable(ConfigurationKind.JDK_AND_ANNOTATIONS, false);
    }

    protected void doTestForGivenFiles(
            Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>> transform,
            boolean includeMembersOfObject,
            List<JetFile> files,
            Predicate<NamespaceDescriptor> filterJetNamespace,
            File serializeResultsTo
    ) {
        ModuleDescriptor module = resolveEagerly(files, new JetCoreEnvironmentWithDisposable(ConfigurationKind.JDK_AND_ANNOTATIONS, false).jetCoreEnvironment);
        ModuleDescriptor lazyModule = resolveLazily(files, stdlibEnvironment.jetCoreEnvironment);

        Pair<NamespaceDescriptor, NamespaceDescriptor> namespacesToCompare = transform.fun(Pair.create(module, lazyModule));

        NamespaceComparator.compareNamespaces(namespacesToCompare.first, namespacesToCompare.second,
                                              includeMembersOfObject, filterJetNamespace, serializeResultsTo);
    }

    public void testStdLib() throws Exception {
        doTestForGivenFiles(
                new Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>>() {
                    @Override
                    public Pair<NamespaceDescriptor, NamespaceDescriptor> fun(Pair<ModuleDescriptor, ModuleDescriptor> pair) {
                        return Pair.create(pair.first.getRootNamespace(), pair.second.getRootNamespace());
                    }
                },
                true,
                //convertToJetFiles(collectKtFiles(new File("compiler/testData/lazyResolve/namespaceComparatorWithJavaMerge"))),
                convertToJetFiles(collectKtFiles(STD_LIB_SRC)),
                Predicates.<NamespaceDescriptor>alwaysTrue(),
                //new Predicate<NamespaceDescriptor>() {
                //    @Override
                //    public boolean apply(NamespaceDescriptor descriptor) {
                //        return Name.identifier("jet").equals(descriptor.getName());
                //    }
                //},
                new File("compiler/testData/lazyResolve/namespaceComparatorWithJavaMerge/stdlib-log.txt")
        );
    }

    private List<JetFile> convertToJetFiles(List<File> files) throws IOException {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (File file : files) {
            JetFile jetFile = JetPsiFactory.createFile(getProject(), file.getName(), FileUtil.loadFile(file, true));
            jetFiles.add(jetFile);
        }
        return jetFiles;
    }

    private List<File> collectKtFiles(@NotNull File root) {
        List<File> files = Lists.newArrayList();
        FileUtil.collectMatchedFiles(root, Pattern.compile(".*?.kt"), files);
        return files;
    }
}
