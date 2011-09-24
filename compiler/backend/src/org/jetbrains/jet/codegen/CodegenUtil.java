package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;

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

    public static ClassDescriptor getOuterClassDescriptor(ClassDescriptor descriptor) {
        DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        while(outerDescriptor != null) {
            if(outerDescriptor instanceof ClassDescriptor)
                break;

            outerDescriptor = outerDescriptor.getContainingDeclaration();
        }
        return (ClassDescriptor) outerDescriptor;
    }
}
