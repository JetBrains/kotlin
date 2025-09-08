// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_JAVA_FACADE
// FILE: ComponentSerializationUtil.java

import org.jetbrains.annotations.NotNull;

public final class ComponentSerializationUtil {
    @NotNull
    public static <S> Class<S> getStateClass(@NotNull Class<? extends PersistentStateComponent> aClass)
    {}
}

// FILE: use.kt

class BeforeRunTask<T>

interface PersistentStateComponent<T>

fun <T> deserializeAndLoadState(
    component: PersistentStateComponent<T>,
    clazz: Class<T> = ComponentSerializationUtil.getStateClass(component::class.java)
) {}

fun use(beforeRunTask: BeforeRunTask<*>) {
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>beforeRunTask is PersistentStateComponent<*><!>) {
        deserializeAndLoadState(beforeRunTask)
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, classReference, flexibleType, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, isExpression, javaFunction, nullableType, smartcast, starProjection,
typeParameter */
