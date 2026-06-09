// IGNORE_BACKEND: JKLIB
// TARGET_BACKEND: JVM

// K1 also sees "fun B<T1>.bar(T1!): T1!", in addition to "fun B<T1>.bar(T1 & Any): T1 & Any". New reflection sees only the latter, which seems more correct.
// KOTLIN_REFLECT_DUMP_MISMATCH

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
