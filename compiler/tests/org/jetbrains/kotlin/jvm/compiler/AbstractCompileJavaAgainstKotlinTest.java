/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jvm.compiler;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier;
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions;
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.util.Collections;

import static org.jetbrains.kotlin.test.KotlinTestUtils.*;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile;

@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors", "JUnitTestCaseWithNoTests"})
public abstract class AbstractCompileJavaAgainstKotlinTest extends TestCaseWithTmpdir {
    // Do not render parameter names because there are test cases where classes inherit from JDK collections,
    // and some versions of JDK have debug information in the class files (including parameter names), and some don't
    private static final RecursiveDescriptorComparator.Configuration CONFIGURATION =
            AbstractLoadJavaTest.COMPARATOR_CONFIGURATION.withRenderer(
                    DescriptorRenderer.Companion.withOptions(
                            new Function1<DescriptorRendererOptions, Unit>() {
                                @Override
                                public Unit invoke(DescriptorRendererOptions options) {
                                    options.setWithDefinedIn(false);
                                    options.setParameterNameRenderingPolicy(ParameterNameRenderingPolicy.NONE);
                                    options.setVerbose(true);
                                    options.setIncludeAnnotationArguments(true);
                                    options.setExcludedAnnotationClasses(Collections.singleton(new FqName(Retention.class.getName())));
                                    options.setModifiers(DescriptorRendererModifier.ALL);
                                    return Unit.INSTANCE;
                                }
                            }
                    )
            );

    protected void doTest(String ktFilePath) throws IOException {
        Assert.assertTrue(ktFilePath.endsWith(".kt"));
        File ktFile = new File(ktFilePath);
        File javaFile = new File(ktFilePath.replaceFirst("\\.kt$", ".java"));
        File expectedFile = new File(ktFilePath.replaceFirst("\\.kt$", ".txt"));
        File javaErrorFile = new File(ktFilePath.replaceFirst("\\.kt$", ".javaerr.txt"));

        File out = new File(tmpdir, "out");
        compileKotlinWithJava(Collections.singletonList(javaFile), Collections.singletonList(ktFile),
                              out, getTestRootDisposable(), javaErrorFile);

        KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, getAnnotationsJar(), out),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        AnalysisResult analysisResult = JvmResolveUtil.analyze(environment);
        PackageViewDescriptor packageView = analysisResult.getModuleDescriptor().getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME);
        assertFalse("Nothing found in package " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, packageView.isEmpty());

        validateAndCompareDescriptorWithFile(packageView, CONFIGURATION, expectedFile);
    }
}
