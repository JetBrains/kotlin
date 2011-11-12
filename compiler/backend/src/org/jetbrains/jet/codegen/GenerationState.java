/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.Stack;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.Collections;
import java.util.List;

public class GenerationState {
    private final ClassFileFactory factory;
    private final Project project;

    private JetTypeMapper typeMapper;
    private final Stack<BindingContext> bindingContexts = new Stack<BindingContext>();
    private final JetStandardLibrary standardLibrary;
    private final IntrinsicMethods intrinsics;

    public GenerationState(Project project, ClassBuilderFactory builderFactory) {
        this.project = project;
        this.standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        this.factory = new ClassFileFactory(builderFactory, this);
        this.intrinsics = new IntrinsicMethods(project, standardLibrary);
    }

    public ClassFileFactory getFactory() {
        return factory;
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
        return factory.newVisitor(typeMapper.jvmName(aClass, OwnerKind.IMPLEMENTATION) + ".class");
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(typeMapper.jvmName(aClass, OwnerKind.TRAIT_IMPL) + ".class");
    }

    public Pair<String, ClassBuilder> forAnonymousSubclass(JetExpression expression) {
        String className = typeMapper.classNameForAnonymousClass(expression);
        return Pair.create(className, factory.forAnonymousSubclass(className));
    }

    public NamespaceCodegen forNamespace(JetNamespace namespace) {
        return factory.forNamespace(namespace);
    }

    public void compile(JetFile psiFile) {
        final JetNamespace namespace = psiFile.getRootNamespace();
        final BindingContext bindingContext = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeNamespace(namespace, JetControlFlowDataTraceFactory.EMPTY);
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        compileCorrectNamespaces(bindingContext, Collections.singletonList(namespace));
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

    public void compileCorrectNamespaces(BindingContext bindingContext, List<JetNamespace> namespaces) {
        compileCorrectNamespaces(bindingContext, namespaces, CompilationErrorHandler.THROW_EXCEPTION);
    }

    public void compileCorrectNamespaces(BindingContext bindingContext, List<JetNamespace> namespaces, CompilationErrorHandler errorHandler) {
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
        bindingContexts.push(bindingContext);
        try {
            for (JetNamespace namespace : namespaces) {
                try {
                    generateNamespace(namespace);
                }
                catch (Throwable e) {
                    errorHandler.reportError("Exception: " + e.getClass().getCanonicalName() + ": " + e.getMessage(), namespace.getContainingFile().getVirtualFile().getUrl());
                }
            }
        }
        finally {
            bindingContexts.pop();
            typeMapper = null;
        }
    }

    protected void generateNamespace(JetNamespace namespace) {
        NamespaceCodegen codegen = forNamespace(namespace);
        codegen.generate(namespace);
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ObjectOrClosureCodegen closure) {
        JetObjectDeclaration objectDeclaration = literal.getObjectDeclaration();
        Pair<String, ClassBuilder> nameAndVisitor = forAnonymousSubclass(objectDeclaration);

        closure.cv = nameAndVisitor.getSecond();
        closure.name = nameAndVisitor.getFirst();
        final CodegenContext objectContext = closure.context.intoAnonymousClass(closure, getBindingContext().get(BindingContext.CLASS, objectDeclaration), OwnerKind.IMPLEMENTATION);

        new ImplementationBodyCodegen(objectDeclaration, objectContext, nameAndVisitor.getSecond(), this).generate(null);

        ConstructorDescriptor constructorDescriptor = closure.state.getBindingContext().get(BindingContext.CONSTRUCTOR, objectDeclaration);
        CallableMethod callableMethod = closure.state.getTypeMapper().mapToCallableMethod(constructorDescriptor, OwnerKind.IMPLEMENTATION);
        return new GeneratedAnonymousClassDescriptor(nameAndVisitor.first, callableMethod.getSignature(), objectContext.outerWasUsed, null);
    }

    public static void prepareAnonymousClasses(JetElement aClass, final JetTypeMapper typeMapper) {
        aClass.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement element) {
                super.visitJetElement(element);
                element.acceptChildren(this);
            }

            @Override
            public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
                typeMapper.classNameForAnonymousClass(expression.getObjectDeclaration());
            }
        });
    }

}
