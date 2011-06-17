package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 * @author yole
 */
public abstract class ClassBodyCodegen {
    protected final BindingContext bindingContext;
    protected final JetStandardLibrary stdlib;
    protected final JetTypeMapper typeMapper;
    protected final JetClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;
    protected final ClassVisitor v;

    public ClassBodyCodegen(BindingContext bindingContext, JetStandardLibrary stdlib, JetClassOrObject aClass, OwnerKind kind, ClassVisitor v) {
        this.bindingContext = bindingContext;
        this.stdlib = stdlib;
        this.typeMapper = new JetTypeMapper(stdlib, bindingContext);
        descriptor = bindingContext.getClassDescriptor(aClass);
        myClass = aClass;
        this.kind = kind;
        this.v = v;
    }

    public void generate() {
        generateDeclaration();

        generateSyntheticParts();

        generateClassBody();

        v.visitEnd();
    }

    protected abstract void generateDeclaration();

    protected void generateSyntheticParts() {
    }

    private void generateClassBody() {
        final FunctionCodegen functionCodegen = new FunctionCodegen((JetDeclaration) myClass, v, stdlib, bindingContext);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, stdlib, bindingContext, functionCodegen);

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }

        generatePrimaryConstructorProperties(propertyCodegen);
    }

    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetProperty) {
            propertyCodegen.gen((JetProperty) declaration, kind);
        }
        else if (declaration instanceof JetFunction) {
            try {
                functionCodegen.gen((JetFunction) declaration, kind);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error generating method " + myClass.getName() + "." + declaration.getName() + " in " + kind, e);
            }
        }
    }

    private void generatePrimaryConstructorProperties(PropertyCodegen propertyCodegen) {
        for (JetParameter p : getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = bindingContext.getPropertyDescriptor(p);
                if (propertyDescriptor != null) {
                    propertyCodegen.generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
                    if (propertyDescriptor.isVar()) {
                        propertyCodegen.generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, kind);
                    }

                    if (!(kind instanceof OwnerKind.DelegateKind) && kind != OwnerKind.INTERFACE && bindingContext.hasBackingField(propertyDescriptor)) {
                        v.visitField(Opcodes.ACC_PRIVATE, p.getName(), typeMapper.mapType(propertyDescriptor.getOutType()).getDescriptor(), null, null);
                    }
                }
            }
        }
    }

    protected List<JetParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof JetClass) {
            return ((JetClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

}
