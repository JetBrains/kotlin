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

package org.jetbrains.jet.cli.jvm.repl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;

/**
 * @author Stepan Koltsov
 */
public class EarlierLine {
    @NotNull
    private final String code;
    @NotNull
    private final ScriptDescriptor scriptDescriptor;
    @NotNull
    private final Class<?> scriptClass;
    @NotNull
    private final Object scriptInstance;
    @NotNull
    private final JvmClassName className;

    public EarlierLine(@NotNull String code, @NotNull ScriptDescriptor scriptDescriptor, @NotNull Class<?> scriptClass, @NotNull Object scriptInstance, @NotNull JvmClassName className) {
        this.code = code;
        this.scriptDescriptor = scriptDescriptor;
        this.scriptClass = scriptClass;
        this.scriptInstance = scriptInstance;
        this.className = className;
    }

    @NotNull
    public String getCode() {
        return code;
    }

    @NotNull
    public ScriptDescriptor getScriptDescriptor() {
        return scriptDescriptor;
    }

    @NotNull
    public Class<?> getScriptClass() {
        return scriptClass;
    }

    @NotNull
    public Object getScriptInstance() {
        return scriptInstance;
    }

    @NotNull
    public JvmClassName getClassName() {
        return className;
    }
}
