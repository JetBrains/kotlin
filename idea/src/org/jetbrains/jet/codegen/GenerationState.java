/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.Stack;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Method;

public class GenerationState {
    private final ClassFileFactory factory;
    private final Project project;

    private JetTypeMapper typeMapper;
    private final Stack<BindingContext> bindingContexts = new Stack<BindingContext>();
    private final Stack<ClosureCodegen> closureContexts = new Stack<ClosureCodegen>();
    private final JetStandardLibrary standardLibrary;

    public GenerationState(Project project, boolean text) {
        this.project = project;
        this.standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        this.factory = new ClassFileFactory(project, text, this);
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

    public ClassVisitor forClassInterface(ClassDescriptor aClass) {
        return factory.newVisitor(JetTypeMapper.jvmNameForInterface(aClass) + ".class");
    }

    public ClassCodegen forClass() {
        return new ClassCodegen(this);
    }

    public ClassVisitor forClassImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(JetTypeMapper.jvmNameForImplementation(aClass) + ".class");
    }

    public ClassVisitor forClassDelegatingImplementation(ClassDescriptor aClass) {
        return factory.newVisitor(JetTypeMapper.jvmNameForDelegatingImplementation(aClass) + ".class");
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
        final JetNamespace namespace = ((JetFile) psiFile).getRootNamespace();
        NamespaceCodegen codegen = forNamespace(namespace);
        final BindingContext bindingContext = AnalyzingUtils.analyzeNamespace(namespace, JetControlFlowDataTraceFactory.EMPTY);
        bindingContexts.push(bindingContext);
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
        try {
            AnalyzingUtils.applyHandler(ErrorHandler.THROW_EXCEPTION, bindingContext);
            codegen.generate(namespace);
        }
        finally {
            bindingContexts.pop();
            typeMapper = null;
        }
    }

    public GeneratedAnonymousClassDescriptor generateClosure(JetFunctionLiteralExpression literal, ExpressionCodegen context, ClassContext classContext) {
        final ClosureCodegen codegen = new ClosureCodegen(this, context, classContext);
        closureContexts.push(codegen);
        try {
            return codegen.gen(literal);
        }
        finally {
            final ClosureCodegen popped = closureContexts.pop();
            assert popped == codegen;
        }
    }

    public GeneratedAnonymousClassDescriptor generateObjectLiteral(JetObjectLiteralExpression literal, ExpressionCodegen context, ClassContext classContext) {
        Pair<String, ClassVisitor> nameAndVisitor = forAnonymousSubclass(literal.getObjectDeclaration());

        final ClassContext objectContext = classContext.intoClass(getBindingContext().getClassDescriptor(literal.getObjectDeclaration()), OwnerKind.IMPLEMENTATION);

        new ImplementationBodyCodegen(literal.getObjectDeclaration(), objectContext, nameAndVisitor.getSecond(), this).generate();
        return new GeneratedAnonymousClassDescriptor(nameAndVisitor.first, new Method("<init>", "()V"), false);
    }

    public StackValue lookupInContext(DeclarationDescriptor d) {
        final ClosureCodegen top = closureContexts.peek();
        return top != null ? top.lookupInContext(d) : null;
    }

    void prepareAnonymousClasses(JetElement aClass) {
        aClass.acceptChildren(new JetVisitor() {
            @Override
            public void visitJetElement(JetElement element) {
                super.visitJetElement(element);
                element.acceptChildren(this);
            }

            @Override
            public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
                getTypeMapper().classNameForAnonymousClass(expression.getObjectDeclaration());
            }
        });
    }
}
