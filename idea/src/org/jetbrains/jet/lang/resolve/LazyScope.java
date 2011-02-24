package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetVisitor;
import org.jetbrains.jet.lang.types.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazyScope extends JetScopeAdapter {
    private final List<JetDeclaration> declarations;

    private Map<String, ClassDescriptor> classDescriptors;

    public LazyScope(JetScope scope, List<JetDeclaration> declarations) {
        super(scope);
        this.declarations = declarations;
    }

    public Map<String, ClassDescriptor> getClassDescriptors() {
        if (classDescriptors == null) {
            classDescriptors = new HashMap<String, ClassDescriptor>();
            for (JetDeclaration declaration : declarations) {
                declaration.accept(new JetVisitor() {
                    @Override
                    public void visitClass(JetClass klass) {
                        classDescriptors.put(klass.getName(), new LazyClassDescriptor(LazyScope.this, klass));
                    }
                });
            }
        }
        return classDescriptors;
    }

    @Override
    public ClassDescriptor getClass(String name) {
        ClassDescriptor classDescriptor = getClassDescriptors().get(name);
        if (classDescriptor != null) {
            return classDescriptor;
        }
        return super.getClass(name);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        throw new UnsupportedOperationException(); // TODO
    }
}
