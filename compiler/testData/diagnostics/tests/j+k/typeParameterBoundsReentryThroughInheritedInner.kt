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

fun nestedArgument(n: Outer<Outer<*>.Nested<*>>.Nested<Base<*>.Inner>) {}

fun outer(o: Outer<*>) {}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, starProjection */
