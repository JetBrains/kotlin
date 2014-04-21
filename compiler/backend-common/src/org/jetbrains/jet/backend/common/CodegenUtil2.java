package org.jetbrains.jet.backend.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Backend-independent utility class that should probably get renamed without '2' once another class of
 * the same name is renamed to CodeGenUtilJvm.
 */
public class CodegenUtil2 {

    private CodegenUtil2() {
    }

    // TODO: consider putting predefined method signatures here too.
    public static final String EQUALS_METHOD_NAME = "equals";
    public static final String TO_STRING_METHOD_NAME = "toString";
    public static final String HASH_CODE_METHOD_NAME = "hashCode";

    @Nullable
    public static FunctionDescriptor getDeclaredFunctionByRawSignature(
            @NotNull ClassDescriptor owner,
            @NotNull Name name,
            @NotNull ClassifierDescriptor returnedClassifier,
            @NotNull ClassifierDescriptor... valueParameterClassifiers
    ) {
        Collection<FunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getFunctions(name);
        for (FunctionDescriptor function : functions) {
            if (!CallResolverUtil.isOrOverridesSynthesized(function)
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClassifiers))
                && rawTypeMatches(function.getReturnType(), returnedClassifier)) {
                return function;
            }
        }
        return null;
    }

    public static FunctionDescriptor getAnyEqualsMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                CodegenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier(EQUALS_METHOD_NAME),
                                                               KotlinBuiltIns.getInstance().getBoolean(),
                                                               anyClass);
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyToStringMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                CodegenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier(TO_STRING_METHOD_NAME),
                                                               KotlinBuiltIns.getInstance().getString());
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyHashCodeMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                CodegenUtil2.getDeclaredFunctionByRawSignature(anyClass, Name.identifier(HASH_CODE_METHOD_NAME),
                                                               KotlinBuiltIns.getInstance().getInt());
        assert function != null;
        return function;
    }

    private static boolean valueParameterClassesMatch(
            @NotNull List<ValueParameterDescriptor> parameters,
            @NotNull List<ClassifierDescriptor> classifiers
    ) {
        if (parameters.size() != classifiers.size()) return false;
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameterDescriptor = parameters.get(i);
            ClassifierDescriptor classDescriptor = classifiers.get(i);
            if (!rawTypeMatches(parameterDescriptor.getType(), classDescriptor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rawTypeMatches(JetType type, ClassifierDescriptor classifier) {
        return type.getConstructor().getDeclarationDescriptor().getOriginal() == classifier.getOriginal();
    }
}
