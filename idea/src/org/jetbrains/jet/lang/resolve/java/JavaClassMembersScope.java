package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.WritableFunctionGroup;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class JavaClassMembersScope implements JetScope {
    private final PsiClass psiClass;
    private final JavaSemanticServices semanticServices;
    private final boolean staticMembers;
    private final DeclarationDescriptor containingDeclaration;

    public JavaClassMembersScope(@NotNull DeclarationDescriptor classDescriptor, PsiClass psiClass, JavaSemanticServices semanticServices, boolean staticMembers) {
        this.containingDeclaration = classDescriptor;
        this.psiClass = psiClass;
        this.semanticServices = semanticServices;
        this.staticMembers = staticMembers;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        for (PsiClass innerClass : psiClass.getAllInnerClasses()) {
            if (name.equals(innerClass.getName())) {
                if (innerClass.hasModifierProperty(PsiModifier.STATIC) != staticMembers) return null;
                return semanticServices.getDescriptorResolver().resolveClass(innerClass);
            }
        }
        return null;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        PsiField field = psiClass.findFieldByName(name, true);
        if (field == null) return null;
        if (field.hasModifierProperty(PsiModifier.STATIC) != staticMembers) {
            return null;
        }

        JetType type = semanticServices.getTypeTransformer().transform(field.getType());
        VariableDescriptorImpl propertyDescriptor = new PropertyDescriptor(
                containingDeclaration,
                Collections.<Attribute>emptyList(),
                field.getName(),
                field.hasModifierProperty(PsiModifier.FINAL) ? null : type,
                type);
        semanticServices.getTrace().recordDeclarationResolution(field, propertyDescriptor);
        return propertyDescriptor;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        WritableFunctionGroup writableFunctionGroup = new WritableFunctionGroup(name);
        PsiMethod[] allMethods = psiClass.getMethods(); // TODO : look into superclasses
        for (PsiMethod method : allMethods) {
            if (method.hasModifierProperty(PsiModifier.STATIC) != staticMembers) {
                continue;
            }
            if (!name.equals(method.getName())) {
                 continue;
            }
            final PsiParameter[] parameters = method.getParameterList().getParameters();

            FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                    JavaDescriptorResolver.JAVA_ROOT,
                    Collections.<Attribute>emptyList(), // TODO
                    name
            );
            functionDescriptor.initialize(
                    Collections.<TypeParameterDescriptor>emptyList(), // TODO
                    semanticServices.getDescriptorResolver().resolveParameterDescriptors(functionDescriptor, parameters),
                    semanticServices.getTypeTransformer().transform(method.getReturnType())
            );
            semanticServices.getTrace().recordDeclarationResolution(method, functionDescriptor);
            writableFunctionGroup.addFunction(functionDescriptor);
        }
        return writableFunctionGroup;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public JetType getThisType() {
        return null;
    }
}
