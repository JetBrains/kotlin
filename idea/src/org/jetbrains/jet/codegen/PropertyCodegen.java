package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.jet.lang.psi.JetConstantExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
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

    public void genInNamespace(JetProperty p) {
       gen(p, OwnerKind.NAMESPACE);
    }

    public void genInInterface(JetProperty p) {
        gen(p, OwnerKind.INTERFACE);
    }

    public void genInImplementation(JetProperty p) {
        gen(p, OwnerKind.IMPLEMENTATION);
    }

    public void genInDelegatingImplementation(JetProperty p) {
        gen(p, OwnerKind.DELEGATING_IMPLEMENTATION);
    }

    public void gen(JetProperty p, OwnerKind kind) {
        if (kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION) {
            final VariableDescriptor descriptor = context.getVariableDescriptor(p);
            if (!(descriptor instanceof PropertyDescriptor)) {
                throw new UnsupportedOperationException("expect a property to have a property descriptor");
            }
            final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            if (context.hasBackingField(propertyDescriptor)) {
                Object value = null;
                final JetExpression initializer = p.getInitializer();
                if (initializer != null) {
                    if (initializer instanceof JetConstantExpression) {
                        value = ((JetConstantExpression) initializer).getValue();
                    }
                }
                int modifiers = Opcodes.ACC_PRIVATE;
                if (kind == OwnerKind.NAMESPACE) {
                    modifiers |= Opcodes.ACC_STATIC;
                }
                v.visitField(modifiers, p.getName(), mapper.mapType(descriptor.getOutType()).getDescriptor(), null, value);
            }
            final JetPropertyAccessor getter = p.getGetter();
            if (getter != null) {
                functionCodegen.generateMethod(getter, kind, mapper.mapGetterSignature(propertyDescriptor),
                        Collections.<ValueParameterDescriptor>emptyList());
            }
            else if (p.hasModifier(JetTokens.PUBLIC_KEYWORD)) {
                generateDefaultGetter(p, kind);
            }
            final JetPropertyAccessor setter = p.getSetter();
            if (setter != null) {
                final PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
                assert setterDescriptor != null;
                functionCodegen.generateMethod(setter, kind, mapper.mapSetterSignature(propertyDescriptor),
                        setterDescriptor.getUnsubstitutedValueParameters());
            }
            else if (p.hasModifier(JetTokens.PUBLIC_KEYWORD) && p.isVar()) {
                generateDefaultSetter(p, kind);
            }
        }
    }

    private void generateDefaultGetter(JetProperty p, OwnerKind kind) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) context.getVariableDescriptor(p);
        int flags = JetTypeMapper.getAccessModifiers(p, Opcodes.ACC_PUBLIC);
        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }
        final String signature = mapper.mapGetterSignature(propertyDescriptor).getDescriptor();
        MethodVisitor mv = v.visitMethod(flags, getterName(p.getName()), signature, null, null);
        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);
        if (kind != OwnerKind.NAMESPACE) {
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
        }
        final Type type = mapper.mapType(propertyDescriptor.getOutType());
        iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
                JetTypeMapper.getOwner(propertyDescriptor), propertyDescriptor.getName(),
                type.getDescriptor());
        iv.areturn(type);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateDefaultSetter(JetProperty p, OwnerKind kind) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) context.getVariableDescriptor(p);
        int flags = JetTypeMapper.getAccessModifiers(p, Opcodes.ACC_PUBLIC);
        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }
        final String signature = mapper.mapSetterSignature(propertyDescriptor).getDescriptor();
        MethodVisitor mv = v.visitMethod(flags, setterName(p.getName()), signature, null, null);
        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);
        final Type type = mapper.mapType(propertyDescriptor.getOutType());
        if (kind != OwnerKind.NAMESPACE) {
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            iv.load(1, type);
        }
        else {
            iv.load(0, type);
        }
        iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD,
                JetTypeMapper.getOwner(propertyDescriptor), propertyDescriptor.getName(),
                type.getDescriptor());
        iv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static String getterName(String propertyName) {
        return "get" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }

    public static String setterName(String propertyName) {
        return "set" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }
}
