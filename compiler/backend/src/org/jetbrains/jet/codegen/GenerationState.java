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

/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectLiteralExpression;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.utils.Progress;

import java.util.Collections;
import java.util.List;

public class GenerationState {
    @NotNull
    private final ClassFileFactory factory;
    private final Project project;

    private JetTypeMapper typeMapper;
    private final Stack<BindingContext> bindingContexts = new Stack<BindingContext>();
    private final JetStandardLibrary standardLibrary;
    private final IntrinsicMethods intrinsics;
    private final Progress progress;

    public GenerationState(Project project, ClassBuilderFactory builderFactory) {
        this(project, builderFactory, Progress.DEAF);
    }

    public GenerationState(Project project, ClassBuilderFactory builderFactory, Progress progress) {
        this.project = project;
        this.progress = progress;
        this.standardLibrary = JetStandardLibrary.getInstance();
        this.factory = new ClassFileFactory(builderFactory, this);
        this.intrinsics = new IntrinsicMethods(project, standardLibrary);
    }

    @NotNull
    public ClassFileFactory getFactory() {
        return factory;
    }

    public Progress getProgress() {
        return progress;
    }

    public Project getProject() {
        return project;
    }

    public JetTypeMapper getTypeMapper() {
        return typeMapper;
    }

    public BindingContext getBindingContext() {
        return bindingContexts.peek();
    }

    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    public IntrinsicMethods getIntrinsics() {
        return intrinsics;
    }

    public ClassCodegen forClass() {
        return new ClassCodegen(this);
    }

    public ClassBuilder forClassImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(typeMapper.mapType(aClass.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName() + ".class");
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(typeMapper.mapType(aClass.getDefaultType(), OwnerKind.TRAIT_IMPL).getInternalName() + ".class");
    }

    public Pair<String, ClassBuilder> forAnonymousSubclass(JetExpression expression) {
        String className = typeMapper.getClosureAnnotator().classNameForAnonymousClass(expression);
        return Pair.create(className, factory.forAnonymousSubclass(className));
    }

    public NamespaceCodegen forNamespace(JetFile namespace) {
        return factory.forNamespace(namespace);
    }

    public BindingContext compile(JetFile file) {
        final BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(file, JetControlFlowDataTraceFactory.EMPTY);
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        compileCorrectFiles(bindingContext, Collections.singletonList(file), CompilationErrorHandler.THROW_EXCEPTION, true);
        return bindingContext;
//        NamespaceCodegen codegen = forNamespace(namespace);
//        bindingContexts.push(bindingContext);
//        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
//        try {
//            AnalyzingUtils.throwExceptionOnErrors(bindingContext);
//
//            codegen.generate(namespace);
//        }
//        finally {
//            bindingContexts.pop();
//            typeMapper = null;
//        }
    }

    public void compileCorrectFiles(BindingContext bindingContext, List<JetFile> files, boolean annotate) {
        compileCorrectFiles(bindingContext, files, CompilationErrorHandler.THROW_EXCEPTION, annotate);
    }

    public void compileCorrectFiles(BindingContext bindingContext, List<JetFile> files, @NotNull CompilationErrorHandler errorHandler, boolean annotate) {
        ClosureAnnotator closureAnnotator = !annotate ? null : new ClosureAnnotator(bindingContext, files);
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext, closureAnnotator);
        bindingContexts.push(bindingContext);
        try {
            for (JetFile file : files) {
                if (file == null) throw new IllegalArgumentException("A null file given for compilation");
                VirtualFile vFile = file.getVirtualFile();
                String path = vFile != null ? vFile.getPath() : "no_virtual_file/" + file.getName();
                progress.log("For source: " + path);
                try {
                    generateNamespace(file);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    errorHandler.reportException(e, vFile == null ? "no file" : vFile.getUrl());
                    DiagnosticUtils.throwIfRunningOnServer(e);
                    if (ApplicationManager.getApplication().isInternal()) {
                        e.printStackTrace();
                    }
                }
            }
        }
        finally {
            bindingContexts.pop();
            typeMapper = null;
        }
    }

    protected void generateNamespace(JetFile namespace) {
        NamespaceCodegen codegen = forNamespace(namespace);
        codegen.generate(namespace);
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ObjectOrClosureCodegen closure) {
        JetObjectDeclaration objectDeclaration = literal.getObjectDeclaration();
        Pair<String, ClassBuilder> nameAndVisitor = forAnonymousSubclass(objectDeclaration);

        closure.cv = nameAndVisitor.getSecond();
        closure.name = nameAndVisitor.getFirst();
        final CodegenContext objectContext = closure.context.intoAnonymousClass(closure, getBindingContext().get(BindingContext.CLASS, objectDeclaration), OwnerKind.IMPLEMENTATION, typeMapper);

        new ImplementationBodyCodegen(objectDeclaration, objectContext, nameAndVisitor.getSecond(), this).generate();

        ConstructorDescriptor constructorDescriptor = closure.state.getBindingContext().get(BindingContext.CONSTRUCTOR, objectDeclaration);
        CallableMethod callableMethod = closure.state.getTypeMapper().mapToCallableMethod(constructorDescriptor, OwnerKind.IMPLEMENTATION, typeMapper.hasThis0(constructorDescriptor.getContainingDeclaration()));
        return new GeneratedAnonymousClassDescriptor(nameAndVisitor.first, callableMethod.getSignature().getAsmMethod(), objectContext.outerWasUsed, null);
    }

    public String createText() {
        StringBuilder answer = new StringBuilder();

        final ClassFileFactory factory = getFactory();
        List<String> files = factory.files();
        for (String file : files) {
//            if (!file.startsWith("kotlin/")) {
                answer.append("@").append(file).append('\n');
                answer.append(factory.asText(file));
//            }
        }

        return answer.toString();
    }
}
