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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.jvm.compiler.NamespaceComparator;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
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
public abstract class AbstractLazyResolveNamespaceComparingTest extends KotlinTestWithEnvironment {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    protected void doTest(
            String testFileName,
            Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>> transform,
            boolean includeMembersOfObject
    ) throws IOException {
        List<JetFile> files = JetTestUtils
                .createTestFiles(testFileName, FileUtil.loadFile(new File(testFileName), true),
                                 new JetTestUtils.TestFileFactory<JetFile>() {
                                     @Override
                                     public JetFile create(String fileName, String text) {
                                         return JetPsiFactory.createFile(getProject(), fileName, text);
                                     }
                                 });

        Predicate<NamespaceDescriptor> filterJetNamespace = new Predicate<NamespaceDescriptor>() {
            @Override
            public boolean apply(NamespaceDescriptor namespaceDescriptor) {
                return !namespaceDescriptor.getName().equals(Name.identifier("jet"));
            }
        };

        File serializeResultsTo = new File(FileUtil.getNameWithoutExtension(testFileName) + ".txt");

        doTestForGivenFiles(transform, includeMembersOfObject, files, filterJetNamespace, serializeResultsTo);
    }

    protected void doTestForGivenFiles(
            Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>> transform,
            boolean includeMembersOfObject,
            List<JetFile> files,
            Predicate<NamespaceDescriptor> filterJetNamespace,
            File serializeResultsTo
    ) {
        ModuleDescriptor module = LazyResolveTestUtil.resolveEagerly(files, getEnvironment());
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(files, getEnvironment());

        Pair<NamespaceDescriptor, NamespaceDescriptor> namespacesToCompare = transform.fun(Pair.create(module, lazyModule));

        NamespaceComparator.compareNamespaces(namespacesToCompare.first, namespacesToCompare.second,
                                              includeMembersOfObject, filterJetNamespace, serializeResultsTo);
    }

    //private ModuleDescriptor resolveLazily(List<JetFile> files, ConfigurationKind configurationKind) {
    //    ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));
    //    JetCoreEnvironmentWithDisposable environment = new JetCoreEnvironmentWithDisposable(configurationKind);
    //    ModuleConfiguration moduleConfiguration = getEagerInjectorForTopDownAnalyzer(environment).getModuleConfiguration();
    //    ResolveSession
    //            session = new ResolveSession(getProject(), lazyModule, moduleConfiguration, new FileBasedDeclarationProviderFactory(files));
    //    return lazyModule;
    //}

    protected void doTest(String testFileName) throws Exception {
        doTest(testFileName, new Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>>() {
            @Override
            public Pair<NamespaceDescriptor, NamespaceDescriptor> fun(Pair<ModuleDescriptor, ModuleDescriptor> pair) {
                return Pair.create(pair.first.getRootNamespace(), pair.second.getRootNamespace());
            }
        }, true);
    }

    protected void doTestSinglePackage(String testFileName) throws Exception {
        doTestSinglePackage(testFileName, false);
    }

    protected void doTestSinglePackage(String testFileName, boolean includeMembersOfObject) throws Exception {
        doTest(testFileName, new Function<Pair<ModuleDescriptor, ModuleDescriptor>, Pair<NamespaceDescriptor, NamespaceDescriptor>>() {
            @Override
            public Pair<NamespaceDescriptor, NamespaceDescriptor> fun(Pair<ModuleDescriptor, ModuleDescriptor> pair) {
                ModuleDescriptor expectedModule = pair.first;
                ModuleDescriptor actualModule = pair.second;
                Name test = Name.identifier("test");
                NamespaceDescriptor actual = actualModule.getRootNamespace().getMemberScope().getNamespace(test);
                NamespaceDescriptor expected = expectedModule.getRootNamespace().getMemberScope().getNamespace(test);
                //NamespaceDescriptor actual = theOnlySubPackage(actualModule.getRootNamespace());
                //NamespaceDescriptor expected = expectedModule.getRootNamespace().getMemberScope().getNamespace(actual.getName());
                return Pair.create(expected, actual);
            }
        }, includeMembersOfObject);
    }

    private NamespaceDescriptor theOnlySubPackage(NamespaceDescriptor namespace) {
        return (NamespaceDescriptor) namespace.getMemberScope().getAllDescriptors().iterator().next();
    }

    public static void main(String[] args) throws IOException {
        String extension = "kt";
        new TestGenerator(
            "compiler/tests/",
            AbstractLazyResolveNamespaceComparingTest.class.getPackage().getName(),
            "LazyResolveNamespaceComparingTestGenerated",
            AbstractLazyResolveNamespaceComparingTest.class,
            Arrays.asList(
                    new SimpleTestClassModel(new File("compiler/testData/readKotlinBinaryClass"),
                                             true,
                                             extension,
                                             "doTestSinglePackage"),
                    new SimpleTestClassModel(new File("compiler/testData/readJavaBinaryClass"),
                                             true,
                                             extension,
                                             "doTestSinglePackage"),
                    new SimpleTestClassModel(new File("compiler/testData/lazyResolve/namespaceComparator"),
                                             true,
                                             extension,
                                             "doTest")
            ),
            AbstractLazyResolveNamespaceComparingTest.class
        ).generateAndSave();
    }
}
