/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.builtins;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleParameters;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier;
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions;
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.AdditionalClassPartsProvider;
import org.jetbrains.kotlin.serialization.deserialization.PlatformDependentDeclarationFilter;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.builtins.BuiltInsPackageFragmentProviderKt.createBuiltInPackageFragmentProvider;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.*;

public class LoadBuiltinsTest extends KotlinTestWithEnvironment {
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_NO_RUNTIME, TestJdkKind.MOCK_JDK);
    }

    public void testBuiltIns() throws Exception {
        RecursiveDescriptorComparator.Configuration configuration =
                RecursiveDescriptorComparator.RECURSIVE_ALL.includeMethodsOfKotlinAny(false).withRenderer(
                        DescriptorRenderer.Companion.withOptions(
                                new Function1<DescriptorRendererOptions, Unit>() {
                                    @Override
                                    public Unit invoke(DescriptorRendererOptions options) {
                                        options.setWithDefinedIn(false);
                                        options.setOverrideRenderingPolicy(OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE);
                                        options.setVerbose(true);
                                        options.setIncludeAnnotationArguments(true);
                                        options.setModifiers(DescriptorRendererModifier.ALL);
                                        return Unit.INSTANCE;
                                    }
                                }
                        )
                );

        PackageFragmentProvider packageFragmentProvider = createBuiltInsPackageFragmentProvider();

        List<KtFile> files = KotlinTestUtils.loadToJetFiles(getEnvironment(), ContainerUtil.concat(
                allFilesUnder("core/builtins/native"),
                allFilesUnder("core/builtins/src")
        ));

        ModuleDescriptor module =
                LazyResolveTestUtilsKt.createResolveSessionForFiles(getEnvironment().getProject(), files, false).getModuleDescriptor();

        for (FqName packageFqName : CollectionsKt.listOf(BUILT_INS_PACKAGE_FQ_NAME, COLLECTIONS_PACKAGE_FQ_NAME, RANGES_PACKAGE_FQ_NAME)) {
            PackageFragmentDescriptor fromLazyResolve =
                    CollectionsKt.single(module.getPackage(packageFqName).getFragments());
            if (fromLazyResolve instanceof LazyPackageDescriptor) {
                PackageFragmentDescriptor deserialized =
                        CollectionsKt.single(packageFragmentProvider.getPackageFragments(packageFqName));
                RecursiveDescriptorComparator.validateAndCompareDescriptors(
                        fromLazyResolve, deserialized, configuration,
                        new File("compiler/testData/builtin-classes/default/" + packageFqName.asString().replace('.', '-') + ".txt")
                );
            }
        }
    }

    @NotNull
    private static PackageFragmentProvider createBuiltInsPackageFragmentProvider() {
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        ModuleDescriptorImpl builtInsModule = new ModuleDescriptorImpl(
                KotlinBuiltIns.BUILTINS_MODULE_NAME, storageManager, ModuleParameters.Empty.INSTANCE, DefaultBuiltIns.getInstance()
        );

        PackageFragmentProvider packageFragmentProvider = createBuiltInPackageFragmentProvider(
                storageManager, builtInsModule, BUILT_INS_PACKAGE_FQ_NAMES,
                new BuiltInFictitiousFunctionClassFactory(storageManager, builtInsModule),
                PlatformDependentDeclarationFilter.All.INSTANCE,
                AdditionalClassPartsProvider.None.INSTANCE,
                new Function1<String, InputStream>() {
                    @Override
                    public InputStream invoke(String path) {
                        return ForTestCompileRuntime.runtimeJarClassLoader().getResourceAsStream(path);
                    }
                }
        );

        builtInsModule.initialize(packageFragmentProvider);
        builtInsModule.setDependencies(builtInsModule);

        return packageFragmentProvider;
    }

    @NotNull
    private static List<File> allFilesUnder(@NotNull String directory) {
        return FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), new File(directory));
    }
}
