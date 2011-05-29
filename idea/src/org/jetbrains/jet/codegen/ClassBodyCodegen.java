package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author max
 * @author yole
 */
public abstract class ClassBodyCodegen {
    protected final BindingContext bindingContext;
    protected final JetStandardLibrary stdlib;
    protected final JetTypeMapper typeMapper;
    protected final JetClass myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;
    protected final ClassVisitor v;

    public ClassBodyCodegen(BindingContext bindingContext, JetStandardLibrary stdlib, JetClass aClass, OwnerKind kind, ClassVisitor v) {
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
        final FunctionCodegen functionCodegen = new FunctionCodegen(myClass, v, stdlib, bindingContext);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(v, stdlib, bindingContext, functionCodegen);

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration, kind);
            }
            else if (declaration instanceof JetFunction) {
                try {
                    functionCodegen.gen((JetFunction) declaration, kind);
                } catch (RuntimeException e) {
                    throw new RuntimeException("Error generating method "+ myClass.getName() + "." + declaration.getName(), e);
                }
            }
        }

        for (JetParameter p : myClass.getPrimaryConstructorParameters()) {
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

}
