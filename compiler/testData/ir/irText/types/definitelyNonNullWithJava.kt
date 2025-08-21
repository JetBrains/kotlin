// FIR_IDENTICAL
// LANGUAGE: +DefinitelyNonNullableTypes
// TARGET_BACKEND: JVM

// FILE: A.java
import org.jetbrains.annotations.*;

public interface A<T> {
    default T foo(T x) { return x; }
    @NotNull
    default T bar(@NotNull T x) { return x; }
}

// FILE: main.kt

interface B<T1> : A<T1> {
    override fun foo(x: T1): T1
    override fun bar(x: T1 & Any): T1 & Any
}
