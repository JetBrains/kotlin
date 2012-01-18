package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author max
 * @author yole
 */
public abstract class ClassBodyCodegen {
    protected final GenerationState state;

    protected final JetClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;
    protected final ClassBuilder v;
    protected final CodegenContext context;

    protected final List<CodeChunk> staticInitializerChunks = new ArrayList<CodeChunk>();

    public ClassBodyCodegen(JetClassOrObject aClass, CodegenContext context, ClassBuilder v, GenerationState state) {
        this.state = state;
        descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        myClass = aClass;
        this.context = context;
        this.kind = context.getContextKind();
        this.v = v;
    }

    public final void generate(@Nullable HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        generateDeclaration();

        generateClassBody();

        generateSyntheticParts(accessors);

        generateStaticInitializer();
    }

    protected abstract void generateDeclaration();

    protected void generateSyntheticParts(HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
    }

    private void generateClassBody() {
        final FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen, state);

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }

        generatePrimaryConstructorProperties(propertyCodegen, myClass);
    }

    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetProperty) {
            propertyCodegen.gen((JetProperty) declaration);
        }
        else if (declaration instanceof JetNamedFunction) {
            try {
                genNamedFunction((JetNamedFunction) declaration, functionCodegen);
            }
            catch(CompilationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new RuntimeException("Error generating method " + myClass.getName() + "." + declaration.getName() + " in " + context, e);
            }
        }
    }

    protected void genNamedFunction(JetNamedFunction declaration, FunctionCodegen functionCodegen) {
        functionCodegen.gen(declaration);
    }

    private void generatePrimaryConstructorProperties(PropertyCodegen propertyCodegen, PsiElement origin) {
        OwnerKind kind = context.getContextKind();
        for (JetParameter p : getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = state.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    propertyCodegen.generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, p);
                    if (propertyDescriptor.isVar()) {
                        propertyCodegen.generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, origin);
                    }

                    //noinspection ConstantConditions
                    if (!(kind instanceof OwnerKind.DelegateKind) && state.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                        v.newField(p, Opcodes.ACC_PRIVATE, p.getName(), state.getTypeMapper().mapType(propertyDescriptor.getOutType()).getDescriptor(), null, null);
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

    private void generateStaticInitializer() {
        if (staticInitializerChunks.size() > 0) {
            final MethodVisitor mv = v.newMethod(null, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                                 "<clinit>", "()V", null, null);
            if (v.generateCode()) {
                mv.visitCode();

                InstructionAdapter v = new InstructionAdapter(mv);

                for (CodeChunk chunk : staticInitializerChunks) {
                    chunk.generate(v);
                }

                mv.visitInsn(Opcodes.RETURN);
                FunctionCodegen.endVisit(v, "static initializer", myClass);
            }
        }
    }
}
