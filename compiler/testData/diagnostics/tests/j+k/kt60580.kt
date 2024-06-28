// FIR_IDENTICAL
// FILE: usage.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

data class P<T1, T2>(val t1: T1, val t2: T2)

fun foo(m: Manager<P<Int, @Anno String>>) {
    m.action {}
}

// FILE: Manager.java
import org.jetbrains.annotations.NotNull;

public interface Manager<T> {
    @NotNull
    Manager<T> action(@NotNull Consumer<? super T> handler);
}

// FILE: Consumer.java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
