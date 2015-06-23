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

package org.jetbrains.kotlin.codegen.state;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.optimization.OptimizationClassBuilderFactory;
import org.jetbrains.kotlin.codegen.when.MappingsClassesForWhenByEnum;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticSink;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GenerationState {
    public interface GenerateClassFilter {
        boolean shouldAnnotateClass(JetClassOrObject classOrObject);
        boolean shouldGenerateClass(JetClassOrObject classOrObject);
        boolean shouldGeneratePackagePart(JetFile jetFile);
        boolean shouldGenerateScript(JetScript script);

        GenerateClassFilter GENERATE_ALL = new GenerateClassFilter() {
            @Override
            public boolean shouldAnnotateClass(JetClassOrObject classOrObject) {
                return true;
            }

            @Override
            public boolean shouldGenerateClass(JetClassOrObject classOrObject) {
                return true;
            }

            @Override
            public boolean shouldGenerateScript(JetScript script) {
                return true;
            }

            @Override
            public boolean shouldGeneratePackagePart(JetFile jetFile) {
                return true;
            }
        };
    }

    private boolean used = false;

    private final Progress progress;
    private final List<JetFile> files;
    private final ClassBuilderMode classBuilderMode;
    private final BindingContext bindingContext;
    private final ClassFileFactory classFileFactory;
    private final Project project;
    private final IntrinsicMethods intrinsics;
    private final SamWrapperClasses samWrapperClasses = new SamWrapperClasses(this);
    private final InlineCycleReporter inlineCycleReporter;
    private final MappingsClassesForWhenByEnum mappingsClassesForWhenByEnum = new MappingsClassesForWhenByEnum(this);
    private final BindingTrace bindingTrace;
    private final JetTypeMapper typeMapper;
    private final boolean disableCallAssertions;
    private final boolean disableParamAssertions;
    private final GenerateClassFilter generateClassFilter;
    private final boolean disableInline;
    private List<ScriptDescriptor> earlierScriptsForReplInterpreter;
    private final ReflectionTypes reflectionTypes;
    private final JvmRuntimeTypes runtimeTypes;
    private final ModuleDescriptor module;
    private final DiagnosticSink diagnostics;
    private final Collection<FqName> packagesWithObsoleteParts;
    private final ClassBuilderFactory interceptedBuilderFactory;

    @Nullable
    private final String moduleId; // for PackageCodegen in incremental compilation mode

    @Nullable
    private final File outDirectory; // TODO: temporary hack, see JetTypeMapperWithOutDirectory state for details

    public GenerationState(
            @NotNull Project project,
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull ModuleDescriptor module,
            @NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files
    ) {
        this(project, builderFactory, Progress.DEAF, module, bindingContext, files, true, true, GenerateClassFilter.GENERATE_ALL,
             false, false, null, null, DiagnosticSink.DO_NOTHING, null);
    }

    public GenerationState(
            @NotNull Project project,
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull Progress progress,
            @NotNull ModuleDescriptor module,
            @NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files,
            boolean disableCallAssertions,
            boolean disableParamAssertions,
            GenerateClassFilter generateClassFilter,
            boolean disableInline,
            boolean disableOptimization,
            @Nullable Collection<FqName> packagesWithObsoleteParts,
            @Nullable String moduleId,
            @NotNull DiagnosticSink diagnostics,
            @Nullable File outDirectory
    ) {
        this.project = project;
        this.progress = progress;
        this.module = module;
        this.files = files;
        this.moduleId = moduleId;
        this.packagesWithObsoleteParts = packagesWithObsoleteParts == null ? Collections.<FqName>emptySet() : packagesWithObsoleteParts;
        this.classBuilderMode = builderFactory.getClassBuilderMode();
        this.disableInline = disableInline;

        this.bindingTrace = new DelegatingBindingTrace(bindingContext, "trace in GenerationState");
        this.bindingContext = bindingTrace.getBindingContext();

        this.outDirectory = outDirectory;
        this.typeMapper = new JetTypeMapperWithOutDirectory(this.bindingContext, classBuilderMode, outDirectory);

        this.intrinsics = new IntrinsicMethods();

        builderFactory = new OptimizationClassBuilderFactory(builderFactory, disableOptimization);

        ClassBuilderFactory interceptedBuilderFactory = new BuilderFactoryForDuplicateSignatureDiagnostics(
                builderFactory, this.bindingContext, diagnostics);

        interceptedBuilderFactory = new BuilderFactoryForDuplicateClassNameDiagnostics(interceptedBuilderFactory, diagnostics);

        Collection<ClassBuilderInterceptorExtension> interceptExtensions =
                ClassBuilderInterceptorExtension.Companion.getInstances(project);

        for (ClassBuilderInterceptorExtension extension : interceptExtensions) {
            interceptedBuilderFactory = extension.interceptClassBuilderFactory(interceptedBuilderFactory, bindingContext, diagnostics);
        }

        this.interceptedBuilderFactory = interceptedBuilderFactory;

        this.diagnostics = diagnostics;
        this.classFileFactory = new ClassFileFactory(this, interceptedBuilderFactory);

        this.disableCallAssertions = disableCallAssertions;
        this.disableParamAssertions = disableParamAssertions;
        this.generateClassFilter = generateClassFilter;

        this.reflectionTypes = new ReflectionTypes(module);
        this.runtimeTypes = new JvmRuntimeTypes();

        this.inlineCycleReporter = new InlineCycleReporter(diagnostics);
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

    @NotNull
    public InlineCycleReporter getInlineCycleReporter() {
        return inlineCycleReporter;
    }

    @NotNull
    public MappingsClassesForWhenByEnum getMappingsClassesForWhenByEnum() {
        return mappingsClassesForWhenByEnum;
    }

    public boolean isCallAssertionsEnabled() {
        return !disableCallAssertions;
    }

    public boolean isParamAssertionsEnabled() {
        return !disableParamAssertions;
    }

    @NotNull
    public GenerateClassFilter getGenerateDeclaredClassFilter() {
        return generateClassFilter;
    }

    @NotNull
    public ReflectionTypes getReflectionTypes() {
        return reflectionTypes;
    }

    @NotNull
    public JvmRuntimeTypes getJvmRuntimeTypes() {
        return runtimeTypes;
    }

    @NotNull
    public DiagnosticSink getDiagnostics() {
        return diagnostics;
    }

    public boolean isInlineEnabled() {
        return !disableInline;
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
        interceptedBuilderFactory.close();
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
    public Collection<FqName> getPackagesWithObsoleteParts() {
        return packagesWithObsoleteParts;
    }

    @Nullable
    public String getModuleId() {
        return moduleId;
    }

    @Nullable
    public File getOutDirectory() {
        return outDirectory;
    }
}
