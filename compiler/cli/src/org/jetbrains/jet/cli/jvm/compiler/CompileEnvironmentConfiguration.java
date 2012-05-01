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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;

import java.util.List;

/**
 * @author abreslav
 */
public class CompileEnvironmentConfiguration {
    @NotNull
    private final JetCoreEnvironment environment;
    @NotNull
    private final MessageCollector messageCollector;

    private List<CompilerPlugin> compilerPlugins = Lists.newArrayList();

    /**
     * NOTE: It's very important to call dispose for every object of this class or there will be memory leaks.
     * @see Disposer
     */
    public CompileEnvironmentConfiguration(@NotNull JetCoreEnvironment environment, @NotNull MessageCollector messageCollector) {
        this.messageCollector = messageCollector;
        this.environment = environment;
    }

    @NotNull
    public JetCoreEnvironment getEnvironment() {
        return environment;
    }

    @NotNull
    public MessageCollector getMessageCollector() {
        return messageCollector;
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }
}
