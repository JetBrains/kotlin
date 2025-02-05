// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK
// FILE: SomeJavaClass.java
import java.util.function.Function;

public class SomeJavaClass<T, R> {
    void doSomething(Function<? super T, ? extends Iterable<? extends R>> block) {}
}

// FILE: test.kt
interface SomeFunInterface<T, R> : (T) -> Set<R>

fun test(
    foo: SomeJavaClass<String, String>,
    bar: SomeFunInterface<String, String>,
) {
    foo.doSomething(bar)
}
