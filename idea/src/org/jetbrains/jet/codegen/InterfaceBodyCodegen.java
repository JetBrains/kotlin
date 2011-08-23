package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class InterfaceBodyCodegen extends ClassBodyCodegen {
    private final List<JetEnumEntry> myEnumConstants = new ArrayList<JetEnumEntry>();

    public InterfaceBodyCodegen(JetClassOrObject aClass, ClassContext context, ClassVisitor v, GenerationState state) {
        super(aClass, context, v, state);
        assert context.getContextKind() == OwnerKind.INTERFACE;
    }

    protected void generateDeclaration() {
        Set<String> superInterfaces = getSuperInterfaces(myClass, state.getBindingContext());

        String fqName = JetTypeMapper.jvmNameForInterface(descriptor);
        v.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                fqName,
                null,
                "java/lang/Object",
                superInterfaces.toArray(new String[superInterfaces.size()])
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    static Set<String> getSuperInterfaces(JetClassOrObject aClass, final BindingContext bindingContext) {
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<String> superInterfaces = new LinkedHashSet<String>();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);

            if (superPsi instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) superPsi;
                String fqn = psiClass.getQualifiedName();
                if (psiClass.isInterface()) {
                    superInterfaces.add(fqn.replace('.', '/'));
                }
                else {
                    if (superClassName == null) {
                        superClassName = fqn.replace('.', '/');

                        while (psiClass != null) {
                            for (PsiClass ifs : psiClass.getInterfaces()) {
                                superInterfaces.add(ifs.getQualifiedName().replace('.', '/'));
                            }
                            psiClass = psiClass.getSuperClass();
                        }
                    }
                    else {
                        throw new RuntimeException("Cannot determine single class to inherit from");
                    }
                }
            }
            else {
                superInterfaces.add(JetTypeMapper.jvmNameForInterface(superClassDescriptor));
            }
        }
        return superInterfaces;
    }

    @Override
    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetEnumEntry && !((JetEnumEntry) declaration).hasPrimaryConstructor()) {
            String name = declaration.getName();
            final String desc = "L" + state.getTypeMapper().jvmName(descriptor, OwnerKind.INTERFACE) + ";";
            v.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, name, desc, null, null);
            if (myEnumConstants.isEmpty()) {
                staticInitializerChunks.add(new CodeChunk() {
                    @Override
                    public void generate(InstructionAdapter v) {
                        initializeEnumConstants(v);
                    }
                });
            }
            myEnumConstants.add((JetEnumEntry) declaration);
        }
        else {
            super.generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }
    }

    private void initializeEnumConstants(InstructionAdapter v) {
        ExpressionCodegen codegen = new ExpressionCodegen(v, new FrameMap(), Type.VOID_TYPE, context, state);
        for (JetEnumEntry enumConstant : myEnumConstants) {
            // TODO type and constructor parameters
            String intfClass = state.getTypeMapper().jvmName(descriptor, OwnerKind.INTERFACE);
            String implClass = state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION);

            final List<JetDelegationSpecifier> delegationSpecifiers = enumConstant.getDelegationSpecifiers();
            if (delegationSpecifiers.size() > 1) {
                throw new UnsupportedOperationException("multiple delegation specifiers for enum constant not supported");
            }

            v.anew(Type.getObjectType(implClass));
            v.dup();

            if (delegationSpecifiers.size() == 1) {
                final JetDelegationSpecifier specifier = delegationSpecifiers.get(0);
                if (specifier instanceof JetDelegatorToSuperCall) {
                    final JetDelegatorToSuperCall superCall = (JetDelegatorToSuperCall) specifier;
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) state.getBindingContext().get(BindingContext.REFERENCE_TARGET, superCall.getCalleeExpression().getConstructorReferenceExpression());
                    CallableMethod method = state.getTypeMapper().mapToCallableMethod(constructorDescriptor, OwnerKind.IMPLEMENTATION);
                    codegen.invokeMethodWithArguments(method, superCall);
                }
                else {
                    throw new UnsupportedOperationException("unsupported type of enum constant initializer: " + specifier);
                }
            }
            else {
                v.invokespecial(implClass, "<init>", "()V");
            }
            v.putstatic(intfClass, enumConstant.getName(), "L" + intfClass + ";");
        }
    }
}
