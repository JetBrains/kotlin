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

package org.jetbrains.jet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.codegen.CompilationException;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.JetTypeName;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ScriptTest {
    @Test
    public void testScript() throws Exception {
        Class<?> aClass = compileScript("fib.ktscript", numIntParam(), Collections.<JetScriptDefinition>emptyList());
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Test
    public void testScriptWithPackage() throws Exception {
        Class<?> aClass = compileScript("fib.pkg.ktscript", numIntParam(), Collections.<JetScriptDefinition>emptyList());
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Test
    public void testScriptStandardExt() throws Exception {
        Class<?> aClass = compileScript("fib.kt", numIntParam(), Collections.<JetScriptDefinition>emptyList());
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Test
    public void testScriptWithScriptDefinition() throws Exception {
        Class<?> aClass = compileScript("fib.fib.kt", null,
                                        Collections.singletonList(new JetScriptDefinition(".fib.kt", numIntParam())));
        Assert.assertNotNull(aClass);
        aClass.getConstructor(int.class).newInstance(4);
    }

    @Nullable
    private static Class<?> compileScript(
            @NotNull String scriptPath,
            @Nullable List<AnalyzerScriptParameter> scriptParameters,
            @NotNull List<JetScriptDefinition> scriptDefinitions
    ) {
        KotlinPaths paths = PathUtil.getKotlinPathsForDistDirectory();
        GroupingMessageCollector messageCollector =
                new GroupingMessageCollector(new PrintingMessageCollector(System.err, MessageRenderer.PLAIN, false));
        Disposable rootDisposable = Disposer.newDisposable();
        try {
            CompilerConfiguration configuration =
                    JetTestUtils.compilerConfigurationForTests(ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.MOCK_JDK);
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, "compiler/testData/script/" + scriptPath);
            configuration.addAll(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, scriptDefinitions);
            configuration.put(JVMConfigurationKeys.SCRIPT_PARAMETERS, scriptParameters);

            JetCoreEnvironment environment = JetCoreEnvironment.createForProduction(rootDisposable, configuration);

            try {
                JetScriptDefinitionProvider.getInstance(environment.getProject()).markFileAsScript(environment.getSourceFiles().get(0));
                return KotlinToJVMBytecodeCompiler.compileScript(paths, environment);
            }
            catch (CompilationException e) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(e),
                                        MessageUtil.psiElementToMessageLocation(e.getElement()));
                return null;
            }
            catch (Throwable t) {
                MessageCollectorUtil.reportException(messageCollector, t);
                return null;
            }
        }
        finally {
            messageCollector.flush();
            Disposer.dispose(rootDisposable);
        }
    }

    @NotNull
    private static List<AnalyzerScriptParameter> numIntParam() {
        return Collections.singletonList(new AnalyzerScriptParameter(Name.identifier("num"), JetTypeName.parse("kotlin.Int")));
    }
}
