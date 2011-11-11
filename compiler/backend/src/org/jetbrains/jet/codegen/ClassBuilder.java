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

public abstract class ClassBuilder {
    public static class Concrete extends ClassBuilder {
        private final ClassVisitor v;

        public Concrete(ClassVisitor v) {
            this.v = v;
        }

        @Override
        public ClassVisitor getVisitor() {
            return v;
        }
    }
    public FieldVisitor newField(@Nullable PsiElement origin,
                                 int access,
                                 String name,
                                 String desc,
                                 @Nullable String signature,
                                 @Nullable Object value) {
        return getVisitor().visitField(access, name, desc, signature, value);
    }

    public MethodVisitor newMethod(@Nullable PsiElement origin,
                                   int access,
                              String name,
                              String desc,
                              @Nullable String signature,
                              @Nullable String[] exceptions) {
        return getVisitor().visitMethod(access, name, desc, signature, exceptions);
    }
    
    public AnnotationVisitor newAnnotation(PsiElement origin,
                                           String desc,
                                           boolean visible) {
        return getVisitor().visitAnnotation(desc, visible);
    }

    public void done() {
        getVisitor().visitEnd();
    }

    public abstract ClassVisitor getVisitor();

    public void defineClass(PsiElement origin, int version, int access, String name, @Nullable String signature, String superName, String[] interfaces) {
        getVisitor().visit(version, access, name, signature, superName, interfaces);
    }

    public void visitSource(String name, @Nullable String debug) {
        getVisitor().visitSource(name, debug);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        getVisitor().visitOuterClass(owner, name, desc);
    }

    public boolean generateCode() {
        return true;
    }
}
