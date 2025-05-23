// RUN_PIPELINE_TILL: BACKEND
// JVM_TARGET: 1.8
// DUMP_INFERENCE_LOGS: FIXATION

// FILE: Function.java
public interface Function<I, O> {
    O fun(I param);
}

// FILE: Renderer.java
public abstract class Renderer<R> {
    public static <S> Renderer<S> create(Function <? super S, String> getText) {
        return null;
    }
}

// FILE: Scheme.java

import org.jetbrains.annotations.*;

public interface Scheme {
    @NotNull default String getDisplayName() {
        return "";
    }
}

// FILE: Manager.java

import org.jetbrains.annotations.*;

public abstract class Manager {
    public abstract Scheme getScheme(@NotNull String schemeName);
}

// FILE: test.kt

fun <T> comboBox(renderer: Renderer<in T?>? = null) {}

fun test() {
    comboBox<String>(
        renderer = Renderer.create { // it should be flexible
            it.substring(1)
        }
    )
}

fun test2(manager: Manager) {
    comboBox<String>(
        renderer = Renderer.create { // it should be flexible
            when (it) {
                "" -> ""
                else -> manager.getScheme(it)?.displayName ?: it
            }
        }
    )
}
