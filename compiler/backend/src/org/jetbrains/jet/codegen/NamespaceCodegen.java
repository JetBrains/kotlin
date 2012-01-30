package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final ClassBuilder v;
    private final GenerationState state;

    public NamespaceCodegen(ClassBuilder v, String fqName, GenerationState state, PsiFile sourceFile) {
        this.v = v;
        this.state = state;

        v.defineClass(sourceFile, V1_6,
                      ACC_PUBLIC/*|ACC_SUPER*/,
                      getJVMClassName(fqName, true),
                      null,
                      //"jet/lang/Namespace",
                      "java/lang/Object",
                      new String[0]
        );
        // TODO figure something out for a namespace that spans multiple files
        v.visitSource(state.transformFileName(sourceFile.getName()), null);
    }

    public void generate(JetFile file) {
        NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.NAMESPACE, file);
        final CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);

        final FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen, state);
        final ClassCodegen classCodegen = state.forClass();

        if (v.generateCode()) {
            GenerationState.prepareAnonymousClasses(file, state.getTypeMapper());
        }

        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration);
            }
            else if (declaration instanceof JetNamedFunction) {
                try {
                    functionCodegen.gen((JetNamedFunction) declaration);
                } catch (CompilationException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new CompilationException("Failed to generate function " + declaration.getName(), e, declaration);
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                classCodegen.generate(context, (JetClassOrObject) declaration);
            }
//            else if (declaration instanceof JetFile) {
//                JetFile childNamespace = (JetFile) declaration;
//                state.forNamespace(childNamespace).generate(childNamespace);
//            }
        }

        if (hasNonConstantPropertyInitializers(file)) {
            generateStaticInitializers(file);
        }

        generateTypeInfoFields(file, context);
    }

    private void generateStaticInitializers(JetFile namespace) {
        MethodVisitor mv = v.newMethod(namespace, ACC_PUBLIC | ACC_STATIC,
                                       "<clinit>", "()V", null, null);
        if (v.generateCode()) {
            mv.visitCode();

            FrameMap frameMap = new FrameMap();
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, CodegenContext.STATIC, state);

            for (JetDeclaration declaration : namespace.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                        final PropertyDescriptor descriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                        assert descriptor != null;
                        if(descriptor.getReceiverParameter().exists()) {
                            continue;
                        }
                        codegen.genToJVMStack(initializer);
                        codegen.intermediateValueForProperty(descriptor, true, null).store(new InstructionAdapter(mv));
                    }
                }
            }

            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "static initializer for namespace", namespace);
            mv.visitEnd();
        }
    }

    private void generateTypeInfoFields(JetFile file, CodegenContext context) {
        if(context.typeInfoConstants != null) {
            String jvmClassName = getJVMClassName(JetPsiUtil.getFQName(file), true);
            for(int index = 0; index != context.typeInfoConstantsCount; index++) {
                JetType type = context.reverseTypeInfoConstants.get(index);
                String fieldName = "$typeInfoCache$" + index;
                v.newField(null, ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, fieldName, "Ljet/TypeInfo;", null, null);

                MethodVisitor mmv = v.newMethod(null, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, "$getCachedTypeInfo$" + index, "()Ljet/TypeInfo;", null, null);
                InstructionAdapter v = new InstructionAdapter(mmv);
                v.visitFieldInsn(GETSTATIC, jvmClassName, fieldName, "Ljet/TypeInfo;");
                v.visitInsn(DUP);
                Label end = new Label();
                v.visitJumpInsn(IFNONNULL, end);

                v.pop();
                generateTypeInfo(context, v, type, state.getTypeMapper(), type);
                v.dup();

                v.visitFieldInsn(PUTSTATIC, jvmClassName, fieldName, "Ljet/TypeInfo;");
                v.visitLabel(end);
                v.visitInsn(ARETURN);
                FunctionCodegen.endVisit(v, "type info method", file);
            }
        }
    }

    private static void generateTypeInfo(CodegenContext context, InstructionAdapter v, JetType jetType, JetTypeMapper typeMapper, JetType root) {
        String knownTypeInfo = typeMapper.isKnownTypeInfo(jetType);
        if(knownTypeInfo != null) {
            v.getstatic("jet/TypeInfo", knownTypeInfo, "Ljet/TypeInfo;");
            return;
        }

        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        if(!jetType.equals(root) && jetType.getArguments().size() == 0 && !JetStandardClasses.getAny().equals(declarationDescriptor)) {
            // TODO: we need some better checks here
            v.getstatic(typeMapper.mapType(jetType, OwnerKind.IMPLEMENTATION).getInternalName(), JvmAbi.TYPE_INFO_FIELD, "Ljet/TypeInfo;");
            return;
        }

        boolean hasUnsubstituted = TypeUtils.hasUnsubstitutedTypeParameters(jetType);
        if(!jetType.equals(root) && !hasUnsubstituted) {
            int typeInfoConstantIndex = context.getTypeInfoConstantIndex(jetType);
            v.invokestatic(context.getNamespaceClassName(), "$getCachedTypeInfo$" + typeInfoConstantIndex, "()Ljet/TypeInfo;");
            return;
        }

        final Type jvmType = typeMapper.mapType(jetType);

        v.aconst(jvmType);
        v.iconst(jetType.isNullable() ? 1 : 0);
        List<TypeProjection> arguments = jetType.getArguments();
        if (arguments.size() > 0 && !(jvmType.getSort() == Type.ARRAY && JetTypeMapper.correctElementType(jvmType).getSort() != Type.OBJECT)) {
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
            v.invokestatic("jet/TypeInfo", JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD, "(Ljava/lang/Class;Z[Ljet/typeinfo/TypeInfoProjection;)Ljet/TypeInfo;");
        }
        else {
            v.invokestatic("jet/TypeInfo", JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD, "(Ljava/lang/Class;Z)Ljet/TypeInfo;");
        }
    }

    private static boolean hasNonConstantPropertyInitializers(JetFile namespace) {
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
        v.done();
    }

    /**
     * @param namespace true for "namespace" suffix 
     */
    public static String getJVMClassName(String fqName, boolean namespace) {
        if (fqName.length() == 0) {
            return JvmAbi.PACKAGE_CLASS;
        }

        String name = fqName.replace('.', '/');
        if(name.startsWith("<java_root>")) {
            name = name.substring("<java_root>".length() + 1, name.length());
        }
        if (namespace) {
            name += "/" + JvmAbi.PACKAGE_CLASS;
        }
        return name;
    }
}
