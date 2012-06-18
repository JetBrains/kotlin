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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.di.InjectorForJvmCodegen;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.utils.Progress;
import org.objectweb.asm.commons.Method;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GenerationState {
    private final Project project;
    private final Progress progress;
    @NotNull
    private final AnalyzeExhaust analyzeExhaust;
    @NotNull
    private final List<JetFile> files;
    @NotNull
    private final InjectorForJvmCodegen injector;
    @NotNull
    private final ClassBuilderMode classBuilderMode;


    private boolean used = false;

    // out parameter
    private Method scriptConstructorMethod;


    public GenerationState(Project project, ClassBuilderFactory builderFactory, AnalyzeExhaust analyzeExhaust, List<JetFile> files) {
        this(project, builderFactory, Progress.DEAF, analyzeExhaust, files, CompilerSpecialMode.REGULAR);
    }

    public GenerationState(Project project, ClassBuilderFactory builderFactory, Progress progress,
            @NotNull AnalyzeExhaust exhaust, @NotNull List<JetFile> files, @NotNull CompilerSpecialMode compilerSpecialMode) {
        this.project = project;
        this.progress = progress;
        this.analyzeExhaust = exhaust;
        this.files = files;
        this.classBuilderMode = builderFactory.getClassBuilderMode();
        this.injector = new InjectorForJvmCodegen(
                analyzeExhaust.getBindingContext(),
                this.files, project, compilerSpecialMode, builderFactory.getClassBuilderMode(), this, builderFactory);
    }

    private void markUsed() {
        if (used) {
            throw new IllegalStateException(
                    GenerationState.class + " cannot be used more than once");
        }
        used = true;
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

    @NotNull
    public ClassBuilderMode getClassBuilderMode() {
        return classBuilderMode;
    }

    public void setScriptConstructorMethod(@NotNull Method scriptConstructorMethod) {
        this.scriptConstructorMethod = scriptConstructorMethod;
    }

    public Method getScriptConstructorMethod() {
        return scriptConstructorMethod;
    }

    public ClassBuilder forClassImplementation(ClassDescriptor aClass) {
        return getFactory().newVisitor(getInjector().getJetTypeMapper().mapType(aClass.getDefaultType(), MapTypeMode.IMPL).getInternalName() + ".class");
    }

    public ClassBuilder forNamespacepart(String name, JetFile file) {
        return getFactory().newVisitor(name + ".class");
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass) {
        return getFactory().newVisitor(getInjector().getJetTypeMapper().mapType(aClass.getDefaultType(), MapTypeMode.TRAIT_IMPL).getInternalName() + ".class");
    }

    public Pair<JvmClassName, ClassBuilder> forAnonymousSubclass(JetExpression expression) {
        JvmClassName className = getInjector().getJetTypeMapper().getClosureAnnotator().classNameForAnonymousClass(expression);
        return Pair.create(className, getFactory().forAnonymousSubclass(className));
    }

    public NamespaceCodegen forNamespace(FqName fqName, Collection<JetFile> jetFiles) {
        return getFactory().forNamespace(fqName, jetFiles);
    }

    private void beforeCompile() {
        markUsed();

        injector.getClosureAnnotator().init();
    }

    public void compileCorrectFiles(@NotNull CompilationErrorHandler errorHandler) {
        for (JetFile file : this.files) {
            if (file.isScript()) {
                injector.getClosureAnnotator().registerClassNameForScript(file.getScript(), ScriptCodegen.SCRIPT_DEFAULT_CLASS_NAME);
            }
        }

        injector.getScriptCodegen().registerEarlierScripts(Collections.<Pair<ScriptDescriptor, JvmClassName>>emptyList());

        beforeCompile();

        MultiMap<FqName, JetFile> namespaceGrouping = new MultiMap<FqName, JetFile>();
        for (JetFile file : this.files) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");
            namespaceGrouping.putValue(JetPsiUtil.getFQName(file), file);
        }

        for (Map.Entry<FqName, Collection<JetFile>> entry : namespaceGrouping.entrySet()) {
            generateNamespace(entry.getKey(), entry.getValue(), errorHandler, progress);
        }
    }

    public void compileScript(
            @NotNull JetScript script,
            @NotNull JvmClassName className,
            @NotNull List<Pair<ScriptDescriptor, JvmClassName>> earlierScripts,
            @NotNull CompilationErrorHandler errorHandler) {

        injector.getScriptCodegen().registerEarlierScripts(earlierScripts);
        injector.getClosureAnnotator().registerClassNameForScript(script, className);

        beforeCompile();

        generateNamespace(
                JetPsiUtil.getFQName((JetFile) script.getContainingFile()),
                Collections.singleton((JetFile) script.getContainingFile()),
                errorHandler,
                progress);
    }

    protected void generateNamespace(FqName fqName, Collection<JetFile> jetFiles, CompilationErrorHandler errorHandler, Progress progress) {
        NamespaceCodegen codegen = forNamespace(fqName, jetFiles);
        codegen.generate(errorHandler, progress);
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ObjectOrClosureCodegen closure) {
        JetObjectDeclaration objectDeclaration = literal.getObjectDeclaration();
        Pair<JvmClassName, ClassBuilder> nameAndVisitor = forAnonymousSubclass(objectDeclaration);

        closure.cv = nameAndVisitor.getSecond();
        closure.name = nameAndVisitor.getFirst();
        final CodegenContext objectContext = closure.context.intoAnonymousClass(
                closure, analyzeExhaust.getBindingContext().get(BindingContext.CLASS, objectDeclaration), OwnerKind.IMPLEMENTATION, injector.getJetTypeMapper());

        new ImplementationBodyCodegen(objectDeclaration, objectContext, nameAndVisitor.getSecond(), this).generate();

        ConstructorDescriptor constructorDescriptor = analyzeExhaust.getBindingContext().get(BindingContext.CONSTRUCTOR, objectDeclaration);
        CallableMethod callableMethod = injector.getJetTypeMapper().mapToCallableMethod(
                constructorDescriptor, OwnerKind.IMPLEMENTATION, injector.getJetTypeMapper().hasThis0(constructorDescriptor.getContainingDeclaration()));
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

    public void destroy() {
        injector.destroy();
    }
}
