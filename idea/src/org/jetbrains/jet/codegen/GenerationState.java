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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;

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

    public Pair<String, ClassVisitor> forClosureIn(ClassDescriptor aClass) {
        return factory.forClosureIn(JetTypeMapper.jvmNameForInterface(aClass));
    }

    public Pair<String, ClassVisitor> forClosureIn(JetNamespace namespace) {
        return factory.forClosureIn(NamespaceCodegen.getJVMClassName(namespace.getFQName()));
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

    public GeneratedClosureDescriptor generateClosure(JetFunctionLiteralExpression literal, ExpressionCodegen context) {
        final ClosureCodegen codegen = new ClosureCodegen(this, context);
        closureContexts.push(codegen);
        try {
            return codegen.gen(literal);
        }
        finally {
            final ClosureCodegen pooped = closureContexts.pop();
            assert pooped == codegen;
        }
    }

    public StackValue lookupInContext(DeclarationDescriptor d) {
        final ClosureCodegen top = closureContexts.peek();
        return top != null ? top.lookupInContext(d) : null;
    }
}
