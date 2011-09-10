package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaClassDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final ClassVisitor v;
    private final GenerationState state;

    public NamespaceCodegen(ClassVisitor v, String fqName, GenerationState state, PsiFile sourceFile) {
        this.v = v;
        this.state = state;

        v.visit(V1_6,
                ACC_PUBLIC,
                getJVMClassName(fqName),
                null,
                //"jet/lang/Namespace",
                "java/lang/Object",
                new String[0]
        );
        // TODO figure something out for a namespace that spans multiple files
        v.visitSource(sourceFile.getName(), null);
    }

    public void generate(JetNamespace namespace) {
        final ClassContext context = ClassContext.STATIC.intoNamespace(state.getBindingContext().get(BindingContext.NAMESPACE, namespace));

        final FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen, state);
        final ClassCodegen classCodegen = state.forClass();

        GenerationState.prepareAnonymousClasses(namespace, state.getTypeMapper());

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration);
            }
            else if (declaration instanceof JetNamedFunction) {
                try {
                    functionCodegen.gen((JetNamedFunction) declaration);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate function " + declaration.getName(), e);
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                classCodegen.generate(context, (JetClassOrObject) declaration);
            }
            else if (declaration instanceof JetNamespace) {
                JetNamespace childNamespace = (JetNamespace) declaration;
                state.forNamespace(childNamespace).generate(childNamespace);
            }
        }

        if (hasNonConstantPropertyInitializers(namespace)) {
            generateStaticInitializers(namespace);
        }

        generateTypeInfoFields(namespace, context);
    }

    private void generateStaticInitializers(JetNamespace namespace) {
        MethodVisitor mv = v.visitMethod(ACC_PUBLIC | ACC_STATIC,
                "<clinit>", "()V", null, null);
        mv.visitCode();

        FrameMap frameMap = new FrameMap();
        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, ClassContext.STATIC, state);

        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                    final PropertyDescriptor descriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                    codegen.genToJVMStack(initializer);
                    codegen.intermediateValueForProperty(descriptor, true, false).store(new InstructionAdapter(mv));
                }
            }
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateTypeInfoFields(JetNamespace namespace, ClassContext context) {
        if(context.typeInfoConstants != null) {
            String jvmClassName = getJVMClassName(namespace.getName());
            for(Map.Entry<JetType,Integer> e : (context.typeInfoConstants != null ? context.typeInfoConstants : Collections.<JetType,Integer>emptyMap()).entrySet()) {
                String fieldName = "$typeInfoCache$" + e.getValue();
                v.visitField(ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC, fieldName, "Ljet/typeinfo/TypeInfo;", null, null);

                MethodVisitor mmv = v.visitMethod(ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC, "$getCachedTypeInfo$" + e.getValue(), "()Ljet/typeinfo/TypeInfo;", null, null);
                InstructionAdapter v = new InstructionAdapter(mmv);
                v.visitFieldInsn(GETSTATIC, jvmClassName, fieldName, "Ljet/typeinfo/TypeInfo;");
                v.visitInsn(DUP);
                Label end = new Label();
                v.visitJumpInsn(IFNONNULL, end);

                v.pop();
                generateTypeInfo(context, v, e.getKey(), state.getTypeMapper(), e.getKey());
                v.dup();

                v.visitFieldInsn(PUTSTATIC, jvmClassName, fieldName, "Ljet/typeinfo/TypeInfo;");
                v.visitLabel(end);
                v.visitInsn(ARETURN);
                v.visitMaxs(0, 0);
                v.visitEnd();
            }
        }
    }

    private static void generateTypeInfo(ClassContext context, InstructionAdapter v, JetType jetType, JetTypeMapper typeMapper, JetType root) {
        String knownTypeInfo = typeMapper.isKnownTypeInfo(jetType);
        if(knownTypeInfo != null) {
            v.getstatic("jet/typeinfo/TypeInfo", knownTypeInfo, "Ljet/typeinfo/TypeInfo;");
            return;
        }

        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        if(!jetType.equals(root) && jetType.getArguments().size() == 0 && !(declarationDescriptor instanceof JavaClassDescriptor)) {
            // TODO: we need some better checks here
            v.getstatic(typeMapper.mapType(jetType, OwnerKind.IMPLEMENTATION).getInternalName(), "$typeInfo", "Ljet/typeinfo/TypeInfo;");
            return;
        }

        boolean hasUnsubstituted = TypeUtils.hasUnsubstitutedTypeParameters(jetType);
        if(!jetType.equals(root) && !hasUnsubstituted) {
            int typeInfoConstantIndex = context.getTypeInfoConstantIndex(jetType);
            v.invokestatic(context.getNamespaceClassName(), "$getCachedTypeInfo$" + typeInfoConstantIndex, "()Ljet/typeinfo/TypeInfo;");
            return;
        }

        final Type jvmType = typeMapper.mapType(jetType, OwnerKind.INTERFACE);

        v.aconst(jvmType);
        v.iconst(jetType.isNullable() ? 1 : 0);
        List<TypeProjection> arguments = jetType.getArguments();
        if (arguments.size() > 0) {
            v.iconst(arguments.size());
            v.newarray(JetTypeMapper.TYPE_TYPEINFOPROJECTION);

            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                TypeProjection argument = arguments.get(i);
                v.dup();
                v.iconst(i);
                generateTypeInfo(context, v, argument.getType(), typeMapper, root);
                ExpressionCodegen.genTypeInfoToProjection(v, argument.getProjectionKind());
                v.astore(JetTypeMapper.TYPE_OBJECT);
            }
            v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z[Ljet/typeinfo/TypeInfoProjection;)Ljet/typeinfo/TypeInfo;");
        }
        else {
            v.invokestatic("jet/typeinfo/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z)Ljet/typeinfo/TypeInfo;");
        }
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
