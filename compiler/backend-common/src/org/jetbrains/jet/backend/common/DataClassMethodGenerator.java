package org.jetbrains.jet.backend.common;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A platform-independent logic for generating data class synthetic methods.
 */
public abstract class DataClassMethodGenerator {
    private final JetClassOrObject myClass;
    private final ClassDescriptor descriptor;
    private final BindingContext bindingContext;

    public DataClassMethodGenerator(JetClassOrObject klazz, ClassDescriptor descriptor, BindingContext bindingContext) {
        this.descriptor = descriptor;
        this.myClass = klazz;
        this.bindingContext = bindingContext;
    }

    public void generate() {
        generateComponentFunctionsForDataClasses();

        generateCopyFunctionForDataClasses(getPrimaryConstructorParameters());

        List<PropertyDescriptor> properties = getDataProperties();
        if (!properties.isEmpty()) {
            generateDataClassToStringIfNeeded(properties);
            generateDataClassHashCodeIfNeeded(properties);
            generateDataClassEqualsIfNeeded(properties);
        }
    }

    // Backend-specific implementations.
    protected abstract void generateComponentFunction(
            @NotNull FunctionDescriptor function,
            @NotNull final ValueParameterDescriptor parameter
    );

    protected abstract void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters);

    protected abstract void generateToStringMethod(@NotNull List<PropertyDescriptor> properties);

    protected abstract void generateHashCodeMethod(@NotNull List<PropertyDescriptor> properties);

    protected abstract void generateEqualsMethod(@NotNull List<PropertyDescriptor> properties);

    protected ClassDescriptor getClassDescriptor() {
        return descriptor;
    }

    private void generateComponentFunctionsForDataClasses() {
        if (!myClass.hasPrimaryConstructor()) return;

        ConstructorDescriptor constructor = descriptor.getConstructors().iterator().next();

        for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
            FunctionDescriptor function = bindingContext.get(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter);
            if (function != null) {
                generateComponentFunction(function, parameter);
            }
        }
    }

    private void generateCopyFunctionForDataClasses(List<JetParameter> constructorParameters) {
        FunctionDescriptor copyFunction = bindingContext.get(BindingContext.DATA_CLASS_COPY_FUNCTION, descriptor);
        if (copyFunction != null) {
            generateCopyFunction(copyFunction, constructorParameters);
        }
    }

    private void generateDataClassToStringIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor stringClass = KotlinBuiltIns.getInstance().getString();
        if (!hasDeclaredNonTrivialMember(CodegenUtil.TO_STRING_METHOD_NAME, stringClass)) {
            generateToStringMethod(properties);
        }
    }

    private void generateDataClassHashCodeIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor intClass = KotlinBuiltIns.getInstance().getInt();
        if (!hasDeclaredNonTrivialMember(CodegenUtil.HASH_CODE_METHOD_NAME, intClass)) {
            generateHashCodeMethod(properties);
        }
    }

    private void generateDataClassEqualsIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor booleanClass = KotlinBuiltIns.getInstance().getBoolean();
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        if (!hasDeclaredNonTrivialMember(CodegenUtil.EQUALS_METHOD_NAME, booleanClass, anyClass)) {
            generateEqualsMethod(properties);
        }
    }

    private List<PropertyDescriptor> getDataProperties() {
        ArrayList<PropertyDescriptor> result = Lists.newArrayList();
        for (JetParameter parameter : getPrimaryConstructorParameters()) {
            if (parameter.getValOrVarNode() != null) {
                result.add(bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter));
            }
        }
        return result;
    }

    private
    @NotNull
    List<JetParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof JetClass) {
            return ((JetClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    /**
     * @return true if the class has a declared member with the given name anywhere in its hierarchy besides Any
     */
    private boolean hasDeclaredNonTrivialMember(
            @NotNull String name,
            @NotNull ClassDescriptor returnedClassifier,
            @NotNull ClassDescriptor... valueParameterClassifiers
    ) {
        FunctionDescriptor function =
                CodegenUtil.getDeclaredFunctionByRawSignature(descriptor, Name.identifier(name), returnedClassifier,
                                                              valueParameterClassifiers);
        if (function == null) {
            return false;
        }

        if (function.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            return true;
        }

        for (CallableDescriptor overridden : OverridingUtil.getOverriddenDeclarations(function)) {
            if (overridden instanceof CallableMemberDescriptor
                && ((CallableMemberDescriptor) overridden).getKind() == CallableMemberDescriptor.Kind.DECLARATION
                && !overridden.getContainingDeclaration().equals(KotlinBuiltIns.getInstance().getAny())) {
                return true;
            }
        }

        return false;
    }
}
