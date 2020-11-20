// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FULL_JDK

// FILE: Single.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Single<T> {

    public T value;

    public Single(T value)
    {
        this.value = value;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE_USE, ElementType.TYPE })
    public @ interface NonNull {}

    @NonNull
    public static <@NonNull T> Single<T> just(T item)
    {
        return new Single (item);
    }
}


// FILE: Kotlin.kt

public inline fun <T> myfold(initial: T, operation: (T) -> T): T {
    return operation(initial)
}

fun box(): String {
    return (myfold(Single.just("O")) { Single.just(it.value + "K") }).value
}

