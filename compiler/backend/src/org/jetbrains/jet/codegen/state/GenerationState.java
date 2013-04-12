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

package org.jetbrains.jet.codegen.state;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.di.InjectorForJvmCodegen;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;

import java.util.List;

public class GenerationState {
    private boolean used = false;

    @NotNull
    private final Progress progress;

    @NotNull
    private final List<JetFile> files;

    @NotNull
    private final ClassBuilderMode classBuilderMode;

    @NotNull
    private final BindingContext bindingContext;

    @NotNull
    private final ClassFileFactory classFileFactory;

    @NotNull
    private final ScriptCodegen scriptCodegen;

    @NotNull
    private final Project project;

    @NotNull
    private final IntrinsicMethods intrinsics;

    @NotNull
    private final BindingTrace bindingTrace;

    @NotNull
    private final JetTypeMapper typeMapper;

    private final boolean generateNotNullAssertions;

    private final boolean generateNotNullParamAssertions;

    private final boolean generateDeclaredClasses;

    public GenerationState(Project project, ClassBuilderFactory builderFactory, BindingContext bindingContext, List<JetFile> files) {
        this(project, builderFactory, Progress.DEAF, bindingContext, files, BuiltinToJavaTypesMapping.ENABLED, true, false, true);
    }

    public GenerationState(
            @NotNull Project project,
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull Progress progress,
            @NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files,
            @NotNull BuiltinToJavaTypesMapping builtinToJavaTypesMapping,
            boolean generateNotNullAssertions,
            boolean generateNotNullParamAssertions,
            boolean generateDeclaredClasses
    ) {
        this.project = project;
        this.progress = progress;
        this.files = files;
        this.classBuilderMode = builderFactory.getClassBuilderMode();

        bindingTrace = new DelegatingBindingTrace(bindingContext, "trace in GenerationState");
        this.bindingContext = bindingTrace.getBindingContext();

        this.typeMapper = new JetTypeMapper(bindingTrace, builtinToJavaTypesMapping == BuiltinToJavaTypesMapping.ENABLED, classBuilderMode);

        InjectorForJvmCodegen injector = new InjectorForJvmCodegen(typeMapper, this, builderFactory, project);

        this.scriptCodegen = injector.getScriptCodegen();
        this.intrinsics = injector.getIntrinsics();
        this.classFileFactory = injector.getClassFileFactory();

        this.generateNotNullAssertions = generateNotNullAssertions;
        this.generateNotNullParamAssertions = generateNotNullParamAssertions;
        this.generateDeclaredClasses = generateDeclaredClasses;
    }

    @NotNull
    public ClassFileFactory getFactory() {
        return classFileFactory;
    }

    @NotNull
    public Progress getProgress() {
        return progress;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public ClassBuilderMode getClassBuilderMode() {
        return classBuilderMode;
    }

    @NotNull
    public List<JetFile> getFiles() {
        return files;
    }

    @NotNull
    public ScriptCodegen getScriptCodegen() {
        return scriptCodegen;
    }

    @NotNull
    public BindingTrace getBindingTrace() {
        return bindingTrace;
    }

    @NotNull
    public JetTypeMapper getTypeMapper() {
        return typeMapper;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public IntrinsicMethods getIntrinsics() {
        return intrinsics;
    }

    public boolean isGenerateNotNullAssertions() {
        return generateNotNullAssertions;
    }

    public boolean isGenerateNotNullParamAssertions() {
        return generateNotNullParamAssertions;
    }

    public boolean isGenerateDeclaredClasses() {
        return generateDeclaredClasses;
    }

    public void beforeCompile() {
        markUsed();

        //noinspection unchecked
        CodegenBinding.initTrace(getBindingTrace(), getFiles());
    }

    private void markUsed() {
        if (used) {
            throw new IllegalStateException(GenerationState.class + " cannot be used more than once");
        }
        used = true;
    }

    public void destroy() {
    }
}
