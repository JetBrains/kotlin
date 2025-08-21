// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

// FILE: A.java
public class A<T> {
    public A<T>[] baz() { return null; }
}


// FILE: main.kt

fun foo1(x: A<*>) = x.baz()
fun foo2(x: A<*>) {
    x.baz() checkType { _<Array<out A<*>>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaType, lambdaLiteral, nullableType, outProjection, starProjection, typeParameter, typeWithExtension */
