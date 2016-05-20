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

package org.jetbrains.kotlin.scripts;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.*;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.codegen.CompilationException;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.script.KotlinScriptDefinition;
import org.jetbrains.kotlin.script.ScriptParameter;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ScriptTest {
    @Test
    public void testScript() throws Exception {
        Class<?> aClass = compileScript("fib.kts", new TestScriptDefinition(".kts", numIntParam()));
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Test
    public void testScriptWithPackage() throws Exception {
        Class<?> aClass = compileScript("fib.pkg.kts", new TestScriptDefinition(".kts", numIntParam()));
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Test
    public void testScriptWithScriptDefinition() throws Exception {
        Class<?> aClass = compileScript("fib.fib.kt", new TestScriptDefinition(".fib.kt", numIntParam()));
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Nullable
    private static Class<?> compileScript(
            @NotNull String scriptPath,
            @NotNull KotlinScriptDefinition scriptDefinition
    ) {
        KotlinPaths paths = PathUtil.getKotlinPathsForDistDirectory();
        MessageCollector messageCollector = PrintingMessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR;

        Disposable rootDisposable = Disposer.newDisposable();
        try {
            CompilerConfiguration configuration =
                    KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK);
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);
            ContentRootsKt.addKotlinSourceRoot(configuration, "compiler/testData/script/" + scriptPath);
            configuration.add(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, scriptDefinition);

            KotlinCoreEnvironment environment =
                    KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

            try {
                return KotlinToJVMBytecodeCompiler.INSTANCE.compileScript(environment, paths);
            }
            catch (CompilationException e) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                                        MessageUtil.psiElementToMessageLocation(e.getElement()));
                return null;
            }
            catch (Throwable t) {
                MessageCollectorUtil.reportException(messageCollector, t);
                return null;
            }
        }
        finally {
            Disposer.dispose(rootDisposable);
        }
    }

    @NotNull
    private static List<ScriptParameter> numIntParam() {
        return Collections.singletonList(new ScriptParameter(Name.identifier("num"), DefaultBuiltIns.getInstance().getIntType()));
    }
}
