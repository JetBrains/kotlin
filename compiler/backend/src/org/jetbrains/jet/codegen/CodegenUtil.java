package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.objectweb.asm.Type;

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

    public static boolean hasTypeInfoField(JetType type) {
        if(type.getConstructor().getParameters().size() > 0)
            return true;

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            if(hasTypeInfoField(jetType))
                return true;
        }

        ClassDescriptor outerClassDescriptor = getOuterClassDescriptor(type.getConstructor().getDeclarationDescriptor());
        if(outerClassDescriptor == null)
            return false;

        return hasTypeInfoField(outerClassDescriptor.getDefaultType());
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
    
    public static Type arrayElementType(Type type) {
        return Type.getType(type.getDescriptor().substring(1));
    }
}
