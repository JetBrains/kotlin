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

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CompileEnvironmentConfiguration;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.codegen.BuiltinToJavaTypesMapping;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;

/**
 * @author abreslav
 */
public class K2JVMCompileEnvironmentConfiguration extends CompileEnvironmentConfiguration {
    private final JetCoreEnvironment environment;
    private final boolean script;
    private final BuiltinsScopeExtensionMode builtinsScopeExtensionMode;
    private final boolean stubs;
    private final BuiltinToJavaTypesMapping builtinToJavaTypesMapping;

    /**
     * NOTE: It's very important to call dispose for every object of this class or there will be memory leaks.
     *
     * @see Disposer
     */
    public K2JVMCompileEnvironmentConfiguration(@NotNull JetCoreEnvironment environment, @NotNull MessageCollector messageCollector,
            boolean script, BuiltinsScopeExtensionMode builtinsScopeExtensionMode, boolean stubs, BuiltinToJavaTypesMapping builtinToJavaTypesMapping) {
        super(messageCollector);
        this.environment = environment;
        this.script = script;
        this.builtinsScopeExtensionMode = builtinsScopeExtensionMode;
        this.stubs = stubs;
        this.builtinToJavaTypesMapping = builtinToJavaTypesMapping;
    }

    public JetCoreEnvironment getEnvironment() {
        return environment;
    }

    public boolean isScript() {
        return script;
    }


    public BuiltinsScopeExtensionMode getBuiltinsScopeExtensionMode() {
        return builtinsScopeExtensionMode;
    }

    public boolean isStubs() {
        return stubs;
    }

    public BuiltinToJavaTypesMapping getBuiltinToJavaTypesMapping() {
        return builtinToJavaTypesMapping;
    }
}
