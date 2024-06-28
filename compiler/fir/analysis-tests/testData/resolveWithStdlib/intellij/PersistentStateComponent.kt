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
    if (<!USELESS_IS_CHECK!>beforeRunTask is PersistentStateComponent<*><!>) {
        deserializeAndLoadState(beforeRunTask)
    }
}

