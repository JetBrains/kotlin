package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final Project project;
    private final ClassVisitor v;
    private final GenerationState state;

    public NamespaceCodegen(Project project, ClassVisitor v, String fqName, GenerationState state) {
        this.project = project;
        this.v = v;
        this.state = state;

        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                getJVMClassName(fqName),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
                new String[0]
        );
    }

    public void generate(JetNamespace namespace) {
        final JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        final FunctionCodegen functionCodegen = new FunctionCodegen(namespace, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, functionCodegen, state);
        final ClassCodegen classCodegen = state.forClass();

        if (hasNonConstantPropertyInitializers(namespace)) {
            generateStaticInitializers(namespace);
        }

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, OwnerKind.NAMESPACE);
            }
            else if (declaration instanceof JetNamedFunction) {
                try {
                    functionCodegen.gen((JetNamedFunction) declaration, OwnerKind.NAMESPACE);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate function " + declaration.getName(), e);
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                classCodegen.generate((JetClassOrObject) declaration);
            }
            else if (declaration instanceof JetNamespace) {
                JetNamespace childNamespace = (JetNamespace) declaration;
                state.forNamespace(childNamespace).generate(childNamespace);
            }
        }
    }

    private void generateStaticInitializers(JetNamespace namespace) {
        MethodVisitor mv = v.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        mv.visitCode();

        FrameMap frameMap = new FrameMap();
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, null, OwnerKind.NAMESPACE, null, state);

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                    final PropertyDescriptor descriptor = (PropertyDescriptor) state.getBindingContext().getVariableDescriptor((JetProperty) declaration);
                    codegen.genToJVMStack(initializer);
                    codegen.intermediateValueForProperty(descriptor, false, false).store(new InstructionAdapter(mv));
                }
            }
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static boolean hasNonConstantPropertyInitializers(JetNamespace namespace) {
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                    return true;
                }

            }
        }
        return false;
    }

    public void done() {
        v.visitEnd();
    }

    public static String getJVMClassName(String fqName) {
        if (fqName.length() == 0) {
            return "namespace";
        }
        return fqName.replace('.', '/') + "/namespace";
    }
}
