// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// CHECK_TYPE

// FILE: Clazz.java
public class Clazz<Psi> {
    public java.util.Collection<Psi> foo() { return null; }
}

// FILE: main.kt

public fun <T, C : MutableCollection<in T>> Iterable<T>.filterTo(destination: C, predicate: (T) -> Boolean) {}

fun test(clazz: Clazz<out Any>) {
    val result = java.util.ArrayList<Any>()
    clazz.foo().filterTo(result) { x -> true }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, infix, javaFunction, javaType, lambdaLiteral, localProperty, nullableType, outProjection,
propertyDeclaration, typeConstraint, typeParameter, typeWithExtension */
