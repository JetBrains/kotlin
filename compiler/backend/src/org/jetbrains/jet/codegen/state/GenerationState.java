/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.reflect.ReflectionTypes;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GenerationState {
    public interface GenerateClassFilter {
        boolean shouldProcess(JetClassOrObject classOrObject);

        GenerateClassFilter GENERATE_ALL = new GenerateClassFilter() {
            @Override
            public boolean shouldProcess(JetClassOrObject classOrObject) {
                return true;
            }
        };
    }

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
    private final Project project;

    @NotNull
    private final IntrinsicMethods intrinsics;

    @NotNull
    private final SamWrapperClasses samWrapperClasses = new SamWrapperClasses(this);

    @NotNull
    private final BindingTrace bindingTrace;

    @NotNull
    private final JetTypeMapper typeMapper;

    private final boolean generateNotNullAssertions;

    private final boolean generateNotNullParamAssertions;

    private final GenerateClassFilter generateClassFilter;

    private final boolean inlineEnabled;

    @Nullable
    private List<ScriptDescriptor> earlierScriptsForReplInterpreter;

    private final JvmRuntimeTypes runtimeTypes;

    @NotNull
    private final ModuleDescriptor module;

    @NotNull
    private final Collection<FqName> packagesWithRemovedFiles;

    @Nullable
    private final String moduleId; // for PackageCodegen in incremental compilation mode

    public GenerationState(
            @NotNull Project project,
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull ModuleDescriptor module,
            @NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files
    ) {
        this(project, builderFactory, Progress.DEAF, module, bindingContext, files, true, false, GenerateClassFilter.GENERATE_ALL,
             InlineCodegenUtil.DEFAULT_INLINE_FLAG, null, null, DiagnosticHolder.DO_NOTHING, null);
    }

    public GenerationState(
            @NotNull Project project,
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull Progress progress,
            @NotNull ModuleDescriptor module,
            @NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files,
            boolean generateNotNullAssertions,
            boolean generateNotNullParamAssertions,
            GenerateClassFilter generateClassFilter,
            boolean inlineEnabled,
            @Nullable Collection<FqName> packagesWithRemovedFiles,
            @Nullable String moduleId,
            @NotNull DiagnosticHolder diagnostics,
            @Nullable File outDirectory
    ) {
        this.project = project;
        this.progress = progress;
        this.module = module;
        this.files = files;
        this.moduleId = moduleId;
        this.packagesWithRemovedFiles = packagesWithRemovedFiles == null ? Collections.<FqName>emptySet() : packagesWithRemovedFiles;
        this.classBuilderMode = builderFactory.getClassBuilderMode();
        this.inlineEnabled = inlineEnabled;

        this.bindingTrace = new DelegatingBindingTrace(bindingContext, "trace in GenerationState");
        this.bindingContext = bindingTrace.getBindingContext();

        this.typeMapper = new JetTypeMapperWithOutDirectory(this.bindingContext, classBuilderMode, outDirectory);

        this.intrinsics = new IntrinsicMethods();
        this.classFileFactory = new ClassFileFactory(this, new BuilderFactoryForDuplicateSignatureDiagnostics(
                builderFactory, this.bindingContext, diagnostics));

        this.generateNotNullAssertions = generateNotNullAssertions;
        this.generateNotNullParamAssertions = generateNotNullParamAssertions;
        this.generateClassFilter = generateClassFilter;

        ReflectionTypes reflectionTypes = new ReflectionTypes(module);
        this.runtimeTypes = new JvmRuntimeTypes(reflectionTypes);
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

    @NotNull
    public SamWrapperClasses getSamWrapperClasses() {
        return samWrapperClasses;
    }

    public boolean isGenerateNotNullAssertions() {
        return generateNotNullAssertions;
    }

    public boolean isGenerateNotNullParamAssertions() {
        return generateNotNullParamAssertions;
    }

    @NotNull
    public GenerateClassFilter getGenerateDeclaredClassFilter() {
        return generateClassFilter;
    }

    @NotNull
    public JvmRuntimeTypes getJvmRuntimeTypes() {
        return runtimeTypes;
    }

    public boolean isInlineEnabled() {
        return inlineEnabled;
    }

    public void beforeCompile() {
        markUsed();

        CodegenBinding.initTrace(this);
    }

    private void markUsed() {
        if (used) {
            throw new IllegalStateException(GenerationState.class + " cannot be used more than once");
        }
        used = true;
    }

    public void destroy() {
    }

    @Nullable
    public List<ScriptDescriptor> getEarlierScriptsForReplInterpreter() {
        return earlierScriptsForReplInterpreter;
    }

    public void setEarlierScriptsForReplInterpreter(@Nullable List<ScriptDescriptor> earlierScriptsForReplInterpreter) {
        this.earlierScriptsForReplInterpreter = earlierScriptsForReplInterpreter;
    }

    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    @NotNull
    public Collection<FqName> getPackagesWithRemovedFiles() {
        return packagesWithRemovedFiles;
    }

    @Nullable
    public String getModuleId() {
        return moduleId;
    }
}
