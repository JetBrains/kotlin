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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating JetLightClasses
 *
 * Consider the following example:
 *
 * KClass.kt refers to JClass.java and vice versa
 *
 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.
 *
 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
public class CliLightClassGenerationSupport extends LightClassGenerationSupport {

    public static CliLightClassGenerationSupport getInstanceForCli(@NotNull Project project) {
        return ServiceManager.getService(project, CliLightClassGenerationSupport.class);
    }

    private BindingTrace trace;

    public CliLightClassGenerationSupport() {
    }

    @NotNull
    public BindingTrace getTrace() {
        if (trace == null) {
            trace = new BindingTraceContext();
        }
        return trace;
    }

    public void clearBindingTrace() {
        assert ApplicationManager.getApplication().isUnitTestMode() : "Mutating project service's state shouldn't happen other than in tests";
        trace = null;
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull JetFile file) {
        return new LightClassConstructionContext(getTrace().getBindingContext(), null);
    }
}
