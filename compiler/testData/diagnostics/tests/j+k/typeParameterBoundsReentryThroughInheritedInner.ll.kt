// LL_FIR_DIVERGENCE
// KT-91970: the compiler misses the violation, as the reentrant enhancement of the bounds of `Nested` gives `S` a wrong bound
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// FILE: Base.java
public class Base<E> {
    public class Inner {}
}

// FILE: Outer.java
public class Outer<T extends Outer.Nested> extends Base<T> {
    public class Nested<S extends Inner> {}
}

// FILE: main.kt
fun nested(n: Outer<*>.Nested<*>) {}

fun nestedArgument(n: Outer<Outer<*>.Nested<*>>.Nested<<!UPPER_BOUND_VIOLATED!>Base<*>.Inner<!>>) {}

fun outer(o: Outer<*>) {}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, starProjection */
