package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;

/**
 * @author max
 */
public class PropertyCodegen {
    private final BindingContext context;
    private final FunctionCodegen functionCodegen;
    private final ClassVisitor v;
    private final JetTypeMapper mapper;

    public PropertyCodegen(ClassVisitor v, JetStandardLibrary standardLibrary, BindingContext context, FunctionCodegen functionCodegen) {
        this.v = v;
        this.context = context;
        this.functionCodegen = functionCodegen;
        this.mapper = new JetTypeMapper(standardLibrary, context);
    }

    public void gen(JetProperty p, OwnerKind kind) {
        final VariableDescriptor descriptor = context.getVariableDescriptor(p);
        if (!(descriptor instanceof PropertyDescriptor)) {
            throw new UnsupportedOperationException("expect a property to have a property descriptor");
        }
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        if (kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.DELEGATING_IMPLEMENTATION) {
            generateBackingField(p, kind, propertyDescriptor);
            generateGetter(p, kind, propertyDescriptor);
            generateSetter(p, kind, propertyDescriptor);
        }
        else if (kind == OwnerKind.INTERFACE) {
            final JetPropertyAccessor getter = p.getGetter();
            if ((getter != null && !getter.hasModifier(JetTokens.PRIVATE_KEYWORD) ||
                 (getter == null && isExternallyAccessible(p)))) {
                v.visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC,
                        getterName(p.getName()),
                        mapper.mapGetterSignature(propertyDescriptor).getDescriptor(),
                        null, null);
            }
            final JetPropertyAccessor setter = p.getSetter();
            if ((setter != null && !setter.hasModifier(JetTokens.PRIVATE_KEYWORD) ||
                (setter == null && isExternallyAccessible(p) && p.isVar()))) {
                v.visitMethod(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC,
                        setterName(p.getName()),
                        mapper.mapSetterSignature(propertyDescriptor).getDescriptor(),
                        null, null);
            }
        }
        else if (kind instanceof OwnerKind.DelegateKind) {
            generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
            if (propertyDescriptor.isVar()) {
                generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
            }
        }
    }

    private void generateBackingField(JetProperty p, OwnerKind kind, PropertyDescriptor propertyDescriptor) {
        if (context.hasBackingField(propertyDescriptor)) {
            Object value = null;
            final JetExpression initializer = p.getInitializer();
            if (initializer != null) {
                if (initializer instanceof JetConstantExpression) {
                    value = ((JetConstantExpression) initializer).getValue();
                }
            }
            final int modifiers;
            if (kind == OwnerKind.NAMESPACE) {
                int access = isExternallyAccessible(p) ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE;
                modifiers = access | Opcodes.ACC_STATIC;
            }
            else {
                modifiers = Opcodes.ACC_PRIVATE;
            }
            v.visitField(modifiers, p.getName(), mapper.mapType(propertyDescriptor.getOutType()).getDescriptor(), null, value);
        }
    }

    private void generateGetter(JetProperty p, OwnerKind kind, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor getter = p.getGetter();
        if (getter != null) {
            if (getter.getBodyExpression() != null) {
                functionCodegen.generateMethod(getter, kind, mapper.mapGetterSignature(propertyDescriptor),
                        null, Collections.<ValueParameterDescriptor>emptyList());
            }
            else if (!getter.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
                generateDefaultGetter(p, getter, kind);
            }
        }
        else if (isExternallyAccessible(p)) {
            generateDefaultGetter(p, p, kind);
        }
    }

    private static boolean isExternallyAccessible(JetProperty p) {
        return !p.hasModifier(JetTokens.PRIVATE_KEYWORD);
    }

    private void generateSetter(JetProperty p, OwnerKind kind, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor setter = p.getSetter();
        if (setter != null) {
            if (setter.getBodyExpression() != null) {
                final PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
                assert setterDescriptor != null;
                functionCodegen.generateMethod(setter, kind, mapper.mapSetterSignature(propertyDescriptor),
                        null, setterDescriptor.getUnsubstitutedValueParameters());
            }
            else if (!p.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
                generateDefaultSetter(p, setter, kind);
            }
        }
        else if (isExternallyAccessible(p) && p.isVar()) {
            generateDefaultSetter(p, p, kind);
        }
    }

    private void generateDefaultGetter(JetProperty p, JetDeclaration declaration, OwnerKind kind) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) context.getVariableDescriptor(p);
        int flags = JetTypeMapper.getAccessModifiers(declaration, Opcodes.ACC_PUBLIC);
        generateDefaultGetter(propertyDescriptor, flags, kind);
    }

    public void generateDefaultGetter(PropertyDescriptor propertyDescriptor, int flags, OwnerKind kind) {
        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }
        else if (kind == OwnerKind.INTERFACE) {
            flags |= Opcodes.ACC_ABSTRACT;
        }

        final String signature = mapper.mapGetterSignature(propertyDescriptor).getDescriptor();
        String getterName = getterName(propertyDescriptor.getName());
        MethodVisitor mv = v.visitMethod(flags, getterName, signature, null, null);
        if (kind != OwnerKind.INTERFACE) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);
            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
            }
            final Type type = mapper.mapType(propertyDescriptor.getOutType());
            if (kind instanceof OwnerKind.DelegateKind) {
                OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                dk.getDelegate().put(JetTypeMapper.TYPE_OBJECT, iv);
                iv.invokeinterface(dk.getOwnerClass(), getterName, signature);
            }
            else {
                iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
                        mapper.getOwner(propertyDescriptor, kind), propertyDescriptor.getName(),
                        type.getDescriptor());
            }
            iv.areturn(type);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private void generateDefaultSetter(JetProperty p, JetDeclaration declaration, OwnerKind kind) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) context.getVariableDescriptor(p);
        int flags = JetTypeMapper.getAccessModifiers(declaration, Opcodes.ACC_PUBLIC);
        generateDefaultSetter(propertyDescriptor, flags, kind);
    }

    public void generateDefaultSetter(PropertyDescriptor propertyDescriptor, int flags, OwnerKind kind) {
        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }
        else if (kind == OwnerKind.INTERFACE) {
            flags |= Opcodes.ACC_ABSTRACT;
        }

        final String signature = mapper.mapSetterSignature(propertyDescriptor).getDescriptor();
        MethodVisitor mv = v.visitMethod(flags, setterName(propertyDescriptor.getName()), signature, null, null);
        if (kind != OwnerKind.INTERFACE) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);
            final Type type = mapper.mapType(propertyDescriptor.getOutType());
            int paramCode = 0;
            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
                paramCode = 1;
            }

            if (kind instanceof OwnerKind.DelegateKind) {
                OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
                dk.getDelegate().put(JetTypeMapper.TYPE_OBJECT, iv);

                iv.load(paramCode, type);
                iv.invokeinterface(dk.getOwnerClass(), setterName(propertyDescriptor.getName()), signature);
            }
            else {
                iv.load(paramCode, type);
                iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD,
                        mapper.getOwner(propertyDescriptor, kind), propertyDescriptor.getName(),
                        type.getDescriptor());
            }

            iv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    public static String getterName(String propertyName) {
        return "get" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }

    public static String setterName(String propertyName) {
        return "set" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }
}
