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

package org.jetbrains.kotlin.asJava;

import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder;
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class AbstractCompilerLightClassTest extends KotlinMultiFileTestWithJava<Void, Void> {
    @NotNull
    @Override
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.ALL;
    }

    @Override
    protected boolean isKotlinSourceRootNeeded() {
        return true;
    }

    @NotNull
    public static JavaElementFinder createFinder(@NotNull KotlinCoreEnvironment environment) throws IOException {
        // We need to resolve all the files in order too fill in the trace that sits inside LightClassGenerationSupport
        KotlinTestUtils.resolveAllKotlinFiles(environment);

        return JavaElementFinder.getInstance(environment.getProject());
    }

    @Override
    protected void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<Void> files) throws IOException {
        KotlinCoreEnvironment environment = createEnvironment(file);
        File expectedFile = KotlinTestUtils.replaceExtension(file, "java");
        LightClassTestCommon.INSTANCE.testLightClass(expectedFile, file, s -> {
            try {
                return createFinder(environment).findClass(s, GlobalSearchScope.allScope(environment.getProject()));
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        }, LightClassTestCommon.INSTANCE::removeEmptyDefaultImpls);
    }

    @Override
    protected Void createTestModule(@NotNull String name) {
        return null;
    }

    @Override
    protected Void createTestFile(Void module, String fileName, String text, Map<String, String> directives) {
        return null;
    }
}
