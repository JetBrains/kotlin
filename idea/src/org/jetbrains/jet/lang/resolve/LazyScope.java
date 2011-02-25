package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetVisitor;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.FunctionGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazyScope extends JetScopeAdapter {
    private final List<JetDeclaration> declarations;

    private Map<String, ClassDescriptor> classDescriptors;
    private Map<String, WritableFunctionGroup> functionGroups;

    public LazyScope(JetScope scope, List<JetDeclaration> declarations) {
        super(scope);
        this.declarations = declarations;
    }

    private Map<String, ClassDescriptor> getClassDescriptors() {
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

    private Map<String, WritableFunctionGroup> getFunctionGroups() {
        if (functionGroups == null) {
            functionGroups = new HashMap<String, WritableFunctionGroup>();
            for (JetDeclaration declaration : declarations) {
                declaration.accept(new JetVisitor() {
                    @Override
                    public void visitFunction(JetFunction function) {
                        FunctionDescriptor functionDescriptor = ClassDescriptorResolver.INSTANCE.resolveFunctionDescriptor(LazyScope.this, function);
                        String name = functionDescriptor.getName();
                        WritableFunctionGroup group = functionGroups.get(name);
                        if (group == null) {
                            group = new WritableFunctionGroup(name);
                            functionGroups.put(name, group);
                        }
                        group.addFunction(functionDescriptor);
                    }
                });
            }
        }
        return functionGroups;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        WritableFunctionGroup group = getFunctionGroups().get(name);
        if (!group.isEmpty()) {
            return group;
        }
        return super.getFunctionGroup(name);
    }
}
