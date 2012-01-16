package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * @author Stepan Koltsov
 */
public class JetMethodAnnotationWriter {
    private final AnnotationVisitor av;

    private JetMethodAnnotationWriter(AnnotationVisitor av) {
        this.av = av;
    }
    
    public void writeKind(int kind) {
        if (kind != JvmStdlibNames.JET_METHOD_KIND_DEFAULT) {
            av.visit(JvmStdlibNames.JET_METHOD_KIND_FIELD, kind);
        }
    }
    
    public void writeTypeParameters(@NotNull String typeParameters) {
        if (typeParameters.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD, typeParameters);
        }
    }

    public void writeReturnType(@NotNull String returnType) {
        if (returnType.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD, returnType);
        }
    }

    public void writePropertyType(@NotNull String propertyType) {
        if (propertyType.length() > 0) {
            av.visit(JvmStdlibNames.JET_METHOD_PROPERTY_TYPE_FIELD, propertyType);
        }
    }
    
    public void writeNullableReturnType(boolean nullableReturnType) {
        if (nullableReturnType) {
            av.visit(JvmStdlibNames.JET_METHOD_NULLABLE_RETURN_TYPE_FIELD, nullableReturnType);
        }
    }
    
    public void visitEnd() {
        av.visitEnd();
    }

    public static JetMethodAnnotationWriter visitAnnotation(MethodVisitor mv) {
        return new JetMethodAnnotationWriter(mv.visitAnnotation(JvmStdlibNames.JET_METHOD.getDescriptor(), true));
    }
}
