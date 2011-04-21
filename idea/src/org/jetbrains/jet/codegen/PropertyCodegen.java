package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.jet.lang.psi.JetConstantExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

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
        if (kind == OwnerKind.NAMESPACE) {
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
                v.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE,
                        p.getName(),
                        mapper.mapType(descriptor.getOutType()).getDescriptor(),
                        null, value);
            }
            final JetPropertyAccessor getter = p.getGetter();
            if (getter != null) {
                functionCodegen.generateMethod(getter, kind, mapper.mapGetterSignature(propertyDescriptor),
                        Collections.<ValueParameterDescriptor>emptyList());
            }
            final JetPropertyAccessor setter = p.getSetter();
            if (setter != null) {
                final PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
                assert setterDescriptor != null;
                functionCodegen.generateMethod(setter, kind, mapper.mapSetterSignature(propertyDescriptor),
                        setterDescriptor.getUnsubstitutedValueParameters());
            }
        }
    }

    public static String getterName(String propertyName) {
        return "get" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }

    public static String setterName(String propertyName) {
        return "set" + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }
}
