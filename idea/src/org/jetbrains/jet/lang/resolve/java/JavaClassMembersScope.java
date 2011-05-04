package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Maps;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author abreslav
 */
public class JavaClassMembersScope implements JetScope {
    private final PsiClass psiClass;
    private final JavaSemanticServices semanticServices;
    private final boolean staticMembers;
    private final DeclarationDescriptor containingDeclaration;
    private final Map<String, FunctionGroup> functionGroups = Maps.newHashMap();
    private final Map<String, VariableDescriptor> variables = Maps.newHashMap();
    private final Map<String, ClassifierDescriptor> classifiers = Maps.newHashMap();

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
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return null;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor classifierDescriptor = classifiers.get(name);
        if (classifierDescriptor == null) {
            classifierDescriptor = doGetClassifierDescriptor(name);
            classifiers.put(name, classifierDescriptor);
        }
        return classifierDescriptor;
    }

    private ClassifierDescriptor doGetClassifierDescriptor(String name) {
        // TODO : suboptimal, walk the list only once
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
        VariableDescriptor variableDescriptor = variables.get(name);
        if (variableDescriptor == null) {
            variableDescriptor = doGetVariable(name);
            variables.put(name, variableDescriptor);
        }
        return variableDescriptor;
    }

    private VariableDescriptor doGetVariable(String name) {
        PsiField field = psiClass.findFieldByName(name, true);
        if (field == null) return null;
        if (field.hasModifierProperty(PsiModifier.STATIC) != staticMembers) {
            return null;
        }

        JetType type = semanticServices.getTypeTransformer().transformToType(field.getType());
        boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                containingDeclaration,
                Collections.<Annotation>emptyList(),
                new MemberModifiers(false, false, false),
                !isFinal,
                field.getName(),
                isFinal ? null : type,
                type);
        semanticServices.getTrace().recordDeclarationResolution(field, propertyDescriptor);
        return propertyDescriptor;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        FunctionGroup functionGroup = functionGroups.get(name);
        if (functionGroup == null) {
            functionGroup = semanticServices.getDescriptorResolver().resolveFunctionGroup(psiClass, name, staticMembers);
            functionGroups.put(name, functionGroup);
        }
        return functionGroup;
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
