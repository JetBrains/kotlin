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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.di.InjectorForJvmCodegen;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectLiteralExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.utils.Progress;

import java.util.List;

public class GenerationState {
    private final Project project;
    private final Progress progress;
    @NotNull
    private final AnalyzeExhaust analyzeExhaust;
    @NotNull
    private final List<JetFile> files;
    @NotNull
    private final InjectorForJvmCodegen injector;


    public GenerationState(Project project, ClassBuilderFactory builderFactory, AnalyzeExhaust analyzeExhaust, List<JetFile> files) {
        this(project, builderFactory, Progress.DEAF, analyzeExhaust, files);
    }

    public GenerationState(Project project,
                           ClassBuilderFactory builderFactory,
                           Progress progress,
                           @NotNull AnalyzeExhaust exhaust,
                           @NotNull List<JetFile> files) {
        this.project = project;
        this.progress = progress;
        this.analyzeExhaust = exhaust;
        this.files = files;
        this.injector =
            new InjectorForJvmCodegen(analyzeExhaust.getStandardLibrary(), analyzeExhaust.getBindingContext(), this.files, project, this,
                                      builderFactory);
    }

    @NotNull
    public ClassFileFactory getFactory() {
        return getInjector().getClassFileFactory();
    }

    public Progress getProgress() {
        return progress;
    }

    public InjectorForJvmCodegen getInjector() {
        return injector;
    }

    public BindingContext getBindingContext() {
        return analyzeExhaust.getBindingContext();
    }

    public ClassCodegen forClass() {
        return new ClassCodegen(this);
    }

    public ClassBuilder forClassImplementation(ClassDescriptor aClass) {
        return getFactory().newVisitor(
            getInjector().getJetTypeMapper().mapType(aClass.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName() + ".class");
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass) {
        return getFactory().newVisitor(
            getInjector().getJetTypeMapper().mapType(aClass.getDefaultType(), OwnerKind.TRAIT_IMPL).getInternalName() + ".class");
    }

    public Pair<String, ClassBuilder> forAnonymousSubclass(JetExpression expression) {
        String className = getInjector().getJetTypeMapper().getClosureAnnotator().classNameForAnonymousClass(expression);
        return Pair.create(className, getFactory().forAnonymousSubclass(className));
    }

    public NamespaceCodegen forNamespace(JetFile namespace) {
        return getFactory().forNamespace(namespace);
    }

    public void compileCorrectFiles(@NotNull CompilationErrorHandler errorHandler) {
        for (JetFile file : this.files) {
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

    protected void generateNamespace(JetFile namespace) {
        NamespaceCodegen codegen = forNamespace(namespace);
        codegen.generate(namespace);
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ObjectOrClosureCodegen closure) {
        JetObjectDeclaration objectDeclaration = literal.getObjectDeclaration();
        Pair<String, ClassBuilder> nameAndVisitor = forAnonymousSubclass(objectDeclaration);

        closure.cv = nameAndVisitor.getSecond();
        closure.name = nameAndVisitor.getFirst();
        final CodegenContext objectContext = closure.context.intoAnonymousClass(
            closure, analyzeExhaust.getBindingContext().get(BindingContext.CLASS, objectDeclaration), OwnerKind.IMPLEMENTATION,
            injector.getJetTypeMapper());

        new ImplementationBodyCodegen(objectDeclaration, objectContext, nameAndVisitor.getSecond(), this).generate();

        ConstructorDescriptor constructorDescriptor = analyzeExhaust.getBindingContext().get(BindingContext.CONSTRUCTOR, objectDeclaration);
        CallableMethod callableMethod = injector.getJetTypeMapper().mapToCallableMethod(
            constructorDescriptor, OwnerKind.IMPLEMENTATION,
            injector.getJetTypeMapper().hasThis0(constructorDescriptor.getContainingDeclaration()));
        return new GeneratedAnonymousClassDescriptor(nameAndVisitor.first, callableMethod.getSignature().getAsmMethod(),
                                                     objectContext.outerWasUsed, null);
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
