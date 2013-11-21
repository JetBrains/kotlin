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

package org.jetbrains.jet.asJava;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;

import java.io.File;
import java.util.List;

public abstract class KotlinAsJavaTestBase extends KotlinTestWithEnvironment {
    protected JavaElementFinder finder;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        CompilerConfiguration configuration = new CompilerConfiguration();

        for (File root : getKotlinSourceRoots()) {
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, root.getPath());
        }

        extraConfiguration(configuration);

        return JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration);
    }

    protected void extraConfiguration(@NotNull CompilerConfiguration configuration) {
    }

    protected abstract List<File> getKotlinSourceRoots();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // We need to resolve all the files in order too fill in the trace that sits inside LightClassGenerationSupport
        JetTestUtils.resolveAllKotlinFiles(getEnvironment());

        finder = JavaElementFinder.getInstance(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        finder = null;
        super.tearDown();
    }

}
