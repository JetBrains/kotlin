/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.Stack;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Method;

public class GenerationState {
    private final ClassFileFactory factory;
    private final Project project;

    private JetTypeMapper typeMapper;
    private final Stack<BindingContext> bindingContexts = new Stack<BindingContext>();
    private final JetStandardLibrary standardLibrary;
    private final IntrinsicMethods intrinsics;

    public GenerationState(Project project, boolean text) {
        this.project = project;
        this.standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        this.factory = new ClassFileFactory(project, text, this);
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

    public ClassVisitor forClassInterface(ClassDescriptor aClass) {
        return factory.newVisitor(JetTypeMapper.jvmNameForInterface(aClass) + ".class");
    }

    public ClassCodegen forClass() {
        return new ClassCodegen(this);
    }

    public ClassVisitor forClassImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(typeMapper.jvmName(aClass, OwnerKind.IMPLEMENTATION) + ".class");
    }

    public ClassVisitor forTraitImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(typeMapper.jvmName(aClass, OwnerKind.TRAIT_IMPL) + ".class");
    }

    public Pair<String, ClassVisitor> forAnonymousSubclass(JetExpression expression) {
        String className = typeMapper.classNameForAnonymousClass(expression);
        ClassVisitor visitor = factory.forAnonymousSubclass(className);
        return Pair.create(className, visitor);
    }

    public NamespaceCodegen forNamespace(JetNamespace namespace) {
        return factory.forNamespace(namespace);
    }

    public void compile(JetFile psiFile) {
        final JetNamespace namespace = psiFile.getRootNamespace();
        NamespaceCodegen codegen = forNamespace(namespace);
        final BindingContext bindingContext = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeNamespace(namespace, JetControlFlowDataTraceFactory.EMPTY);
        bindingContexts.push(bindingContext);
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
        try {
            ErrorHandler.applyHandler(ErrorHandler.THROW_EXCEPTION, bindingContext);
            codegen.generate(namespace);
        }
        finally {
            bindingContexts.pop();
            typeMapper = null;
        }
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ExpressionCodegen context, ClassContext classContext) {
        Pair<String, ClassVisitor> nameAndVisitor = forAnonymousSubclass(literal.getObjectDeclaration());

        final ClassContext objectContext = classContext.intoClass(getBindingContext().get(BindingContext.CLASS, literal.getObjectDeclaration()), OwnerKind.IMPLEMENTATION);

        new ImplementationBodyCodegen(literal.getObjectDeclaration(), objectContext, nameAndVisitor.getSecond(), this).generate();
        return new GeneratedAnonymousClassDescriptor(nameAndVisitor.first, new Method("<init>", "()V"), false);
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
