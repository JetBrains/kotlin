package org.jetbrains.jet.codegen;

import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public class CodegenUtil {
    private CodegenUtil() {
    }

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor)descriptor).getKind() == ClassKind.TRAIT;
    }

    public static boolean isInterface(JetType type) {
        return isInterface(type.getConstructor().getDeclarationDescriptor());
    }
    
    public static boolean isClassObject(DeclarationDescriptor descriptor) {
        if(descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if(classDescriptor.getKind() == ClassKind.OBJECT) {
                if(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                    ClassDescriptor containingDeclaration = (ClassDescriptor) classDescriptor.getContainingDeclaration();
                    if(classDescriptor.getDefaultType().equals(containingDeclaration.getClassObjectType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasThis0(ClassDescriptor classDescriptor) {
        return getOuterClassDescriptor(classDescriptor) != null && !isClassObject(classDescriptor);
    }

    public static ClassDescriptor getOuterClassDescriptor(DeclarationDescriptor descriptor) {
        DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        while(outerDescriptor != null) {
            if(outerDescriptor instanceof ClassDescriptor)
                break;

            outerDescriptor = outerDescriptor.getContainingDeclaration();
        }
        return (ClassDescriptor) outerDescriptor;
    }

    public static boolean hasOuterTypeInfo(ClassDescriptor descriptor) {
        ClassDescriptor outerClassDescriptor = getOuterClassDescriptor(descriptor);
        if(outerClassDescriptor == null)
            return false;

        if(outerClassDescriptor.getTypeConstructor().getParameters().size() > 0)
            return true;

        return hasOuterTypeInfo(outerClassDescriptor);
    }

    public static boolean hasDerivedTypeInfoField(JetType type, boolean exceptOwn) {
        if(!exceptOwn) {
            if(!isInterface(type))
                if(hasTypeInfoField(type))
                    return true;
        }

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            if(hasDerivedTypeInfoField(jetType, false))
                return true;
        }

        return false;
    }

    public static boolean hasTypeInfoField(JetType type) {
        if(isInterface(type))
            return false;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        for (TypeParameterDescriptor parameter : parameters) {
            if(parameter.isReified())
                return true;
        }

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            if(hasTypeInfoField(jetType))
                return true;
        }

        ClassDescriptor outerClassDescriptor = getOuterClassDescriptor(type.getConstructor().getDeclarationDescriptor());
        if(outerClassDescriptor == null || isClassObject(type.getConstructor().getDeclarationDescriptor()))
            return false;

        return hasTypeInfoField(outerClassDescriptor.getDefaultType());
    }

    public static String getFQName(JetNamespace jetNamespace) {
        JetNamespace parent = PsiTreeUtil.getParentOfType(jetNamespace, JetNamespace.class);
        if (parent != null) {
            String parentFQName = getFQName(parent);
            if (parentFQName.length() > 0) {
                return parentFQName + "." + getFQName(jetNamespace.getHeader());
            }
        }
        return getFQName(jetNamespace.getHeader()); // TODO: Must include module root namespace
    }

    private static String getFQName(JetNamespaceHeader header) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<JetSimpleNameExpression> iterator = header.getParentNamespaceNames().iterator(); iterator.hasNext(); ) {
            JetSimpleNameExpression nameExpression = iterator.next();
            builder.append(nameExpression.getReferencedName());
            builder.append(".");
        }
//        PsiElement nameIdentifier = header.getNameIdentifier();
        builder.append(header.getName());
        return builder.toString();
    }

    public static String getFQName(JetClass jetClass) {
        JetNamedDeclaration parent = PsiTreeUtil.getParentOfType(jetClass, JetNamespace.class, JetClass.class);
        if (parent instanceof JetNamespace) {
            return getFQName(((JetNamespace) parent)) + "." + jetClass.getName();
        }
        if (parent instanceof JetClass) {
            return getFQName(((JetClass) parent)) + "." + jetClass.getName();
        }
        return jetClass.getName();
    }

    public static FunctionDescriptor createInvoke(ExpressionAsFunctionDescriptor fd) {
        int arity = fd.getValueParameters().size();
        FunctionDescriptorImpl invokeDescriptor = new FunctionDescriptorImpl(
                fd.getExpectedThisObject().exists() ? JetStandardClasses.getReceiverFunction(arity) : JetStandardClasses.getFunction(arity),
                Collections.<AnnotationDescriptor>emptyList(),
                "invoke");

        invokeDescriptor.initialize(fd.getReceiverParameter().exists() ? fd.getReceiverParameter().getType() : null,
                                   fd.getExpectedThisObject(),
                                   Collections.<TypeParameterDescriptor>emptyList(),
                                   fd.getValueParameters(),
                                   fd.getReturnType(),
                                   Modality.FINAL, Visibility.PUBLIC);
        return invokeDescriptor;
    }

    public static boolean isSubclass(ClassDescriptor subClass, ClassDescriptor superClass) {
        Set<JetType> allSuperTypes = new THashSet<JetType>();

        addSuperTypes(subClass.getDefaultType(), allSuperTypes);

        final DeclarationDescriptor superOriginal = superClass.getOriginal();

        for (JetType superType : allSuperTypes) {
            final DeclarationDescriptor descriptor = superType.getConstructor().getDeclarationDescriptor();
            if (descriptor != null && superOriginal.equals(descriptor.getOriginal())) {
                return true;
            }
        }

        return false;
    }

    private static void addSuperTypes(JetType type, Set<JetType> set) {
        set.add(type);

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            addSuperTypes(jetType, set);
        }
    }

}
