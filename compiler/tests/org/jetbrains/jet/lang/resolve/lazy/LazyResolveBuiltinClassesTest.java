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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.NamespaceComparator;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazyResolveBuiltinClassesTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Test
    public void testJetStandardLibrary() throws Exception {
        List<JetFile> files = Lists.newArrayList();
        addJetFilesFromDir(files, new File("compiler/frontend/src/jet"));

        final Map<FqName, Name> aliases = ImmutableMap.<FqName, Name>builder()
                .put(new FqName("jet.Unit"), Name.identifier("Tuple0"))
                .build();

        ModuleDescriptor lazyModule = new ModuleDescriptor(Name.special("<lazy module>"));
        ResolveSession session = new ResolveSession(
                getProject(),
                lazyModule,
                new SpecialModuleConfiguration(),
                new FileBasedDeclarationProviderFactory(files),
                new Function<FqName, Name>() {
                    @Override
                    public Name fun(FqName name) {
                        return aliases.get(name);
                    }
                },
                Predicates.in(Sets.newHashSet(new FqNameUnsafe("jet.Any"), new FqNameUnsafe("jet.Nothing"))),
                new BindingTraceContext());

        NamespaceDescriptor jetNamespaceFromJSL =
                (NamespaceDescriptor) KotlinBuiltIns.getInstance().getInt().getContainingDeclaration();
        NamespaceDescriptor jetNamespaceFromLazy = lazyModule.getRootNamespace().getMemberScope().getNamespace(
                jetNamespaceFromJSL.getName());

        NamespaceComparator.compareNamespaces(jetNamespaceFromJSL, jetNamespaceFromLazy,
                                              NamespaceComparator.RECURSIVE,
                                              new File("compiler/testData/builtin-classes.txt"));

    }

    private void addJetFilesFromDir(List<JetFile> files, File jetDir) throws IOException {
        for (File file : jetDir.listFiles()) {
            if (FileUtil.getExtension(file.getName()).equals("jet")) {
                files.add(JetPsiFactory.createFile(getProject(), file.getName(), FileUtil.loadFile(file, true)));
            }
        }
    }

    private class SpecialModuleConfiguration implements ModuleConfiguration {
        @Override
        public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
            for (ImportPath defaultJetImport : DefaultModuleConfiguration.DEFAULT_JET_IMPORTS) {
                directives.add(JetPsiFactory.createImportDirective(getProject(), defaultJetImport));
            }
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace,
                @NotNull NamespaceDescriptor namespaceDescriptor,
                @NotNull WritableScope namespaceMemberScope) {
            // DO nothing
        }

        @NotNull
        @Override
        public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
            return JavaToKotlinClassMap.getInstance();
        }
    }
}
