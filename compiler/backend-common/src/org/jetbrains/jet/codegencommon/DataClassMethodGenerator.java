package org.jetbrains.jet.codegencommon;

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
public class DataClassMethodGenerator {

    private final Backend platform;
    private final JetClassOrObject myClass;
    private final ClassDescriptor descriptor;
    private final BindingContext bindingContext;

    public DataClassMethodGenerator(JetClassOrObject klazz, ClassDescriptor descriptor,BindingContext bindingContext, Backend platform) {
        this.descriptor = descriptor;
        this.platform = platform;
        this.myClass = klazz;
        this.bindingContext = bindingContext;
    }

    public interface Backend {
        void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull final ValueParameterDescriptor parameter);
        void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters);
        void generateToStringMethod(@NotNull List<PropertyDescriptor> properties);
        void generateHashCodeMethod(@NotNull List<PropertyDescriptor> properties);
        void generateEqualsMethod(@NotNull List<PropertyDescriptor> properties);
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

    private void generateComponentFunctionsForDataClasses() {
        if (!myClass.hasPrimaryConstructor()) return;

        ConstructorDescriptor constructor = descriptor.getConstructors().iterator().next();

        for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
            FunctionDescriptor function = bindingContext.get(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter);
            if (function != null) {
                platform.generateComponentFunction(function, parameter);
            }
        }
    }

    private void generateCopyFunctionForDataClasses(List<JetParameter> constructorParameters) {
        FunctionDescriptor copyFunction = bindingContext.get(BindingContext.DATA_CLASS_COPY_FUNCTION, descriptor);
        if (copyFunction != null) {
            platform.generateCopyFunction(copyFunction, constructorParameters);
        }
    }

    private void generateDataClassToStringIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor stringClass = KotlinBuiltIns.getInstance().getString();
        if (!hasDeclaredNonTrivialMember("toString", stringClass)) {
            platform.generateToStringMethod(properties);
        }
    }

    private void generateDataClassHashCodeIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor intClass = KotlinBuiltIns.getInstance().getInt();
        if (!hasDeclaredNonTrivialMember("hashCode", intClass)) {
            platform.generateHashCodeMethod(properties);
        }
    }

    private void generateDataClassEqualsIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        ClassDescriptor booleanClass = KotlinBuiltIns.getInstance().getBoolean();
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        if (!hasDeclaredNonTrivialMember("equals", booleanClass, anyClass)) {
            platform.generateEqualsMethod(properties);
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

    private @NotNull List<JetParameter> getPrimaryConstructorParameters() {
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
                CodeGenUtil2.getDeclaredFunctionByRawSignature(descriptor, Name.identifier(name), returnedClassifier,
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

    public static FunctionDescriptor getAnyEqualsMethod(KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function =
                CodeGenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier("equals"), builtIns.getBoolean(),
                                                               anyClass);
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyToStringMethod(KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function =
                CodeGenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier("toString"), builtIns.getString());
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyHashCodeMethod(KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function =
                CodeGenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier("hashCode"), builtIns.getInt());
        assert function != null;
        return function;
    }
}
