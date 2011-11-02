/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassBuilder {
    private final ClassVisitor v;

    public ClassBuilder(ClassVisitor v) {
        this.v = v;
    }

    public FieldVisitor newField(@Nullable PsiElement origin,
                                 int access,
                                 String name,
                                 String desc,
                                 String signature,
                                 Object value) {
        return v.visitField(access, name, desc, signature, value);
    }

    public MethodVisitor newMethod(@Nullable PsiElement origin,
                                   int access,
                              String name,
                              String desc,
                              @Nullable String signature,
                              @Nullable String[] exceptions) {
        return v.visitMethod(access, name, desc, signature, exceptions);
    }
    
    public AnnotationVisitor newAnnotation(PsiElement origin,
                                           String desc,
                                           boolean visible) {
        return v.visitAnnotation(desc, visible);
    }

    public void done() {
        v.visitEnd();
    }

    public ClassVisitor getVisitor() {
        return v;
    }

    public void defineClass(int version, int access, String name, String signature, String superName, String[] interfaces) {
        v.visit(version, access, name, signature, superName, interfaces);
    }

    public void visitSource(String name, String debug) {
        v.visitSource(name, debug);
    }
}
