// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// SAM_CONVERSIONS: INDY
// ISSUE: KT-89915

// FILE: Fn.java
import org.jetbrains.annotations.NotNull;

public interface Fn<T, R> {
    R apply(@NotNull T t);
}

// FILE: box.kt

fun call(fn: Fn<in Result<String>, String>): String = fn.apply(Result.success("OK"))

fun box(): String {
    return call { it.getOrThrow() }
}
