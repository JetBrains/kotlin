package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetEnumEntry;
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
    }

    static Set<String> getSuperInterfaces(JetClassOrObject aClass, final BindingContext bindingContext) {
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        String superClassName = null;
        Set<String> superInterfaces = new LinkedHashSet<String>();
        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            JetType superType = bindingContext.resolveTypeReference(specifier.getTypeReference());
            ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
            PsiElement superPsi = bindingContext.getDeclarationPsiElement(superClassDescriptor);

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
        for (JetEnumEntry enumConstant : myEnumConstants) {
            // TODO type and constructor parameters
            String intfClass = state.getTypeMapper().jvmName(descriptor, OwnerKind.INTERFACE);
            String implClass = state.getTypeMapper().jvmName(descriptor, OwnerKind.IMPLEMENTATION);
            v.anew(Type.getObjectType(implClass));
            v.dup();
            v.invokespecial(implClass, "<init>", "()V");
            v.putstatic(intfClass, enumConstant.getName(), "L" + intfClass + ";");
        }
    }
}
