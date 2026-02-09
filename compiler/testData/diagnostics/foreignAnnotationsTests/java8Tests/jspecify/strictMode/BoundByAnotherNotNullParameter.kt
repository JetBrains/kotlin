// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// FIR_DUMP
// DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: ClassWithTwoTypeParameters.java
import org.jspecify.annotations.*;

public interface ClassWithTwoTypeParameters {
    <FIRST extends @NonNull SECOND, SECOND> FIRST foo(FIRST a, SECOND b);
}

// FILE: test.kt

fun <B, A : B & Any> test(instance: ClassWithTwoTypeParameters, a: A, b: B, s: String) {
    val x = instance.foo<String, CharSequence>(s, "")
    val y = instance.foo<A, B>(a, b)
}
