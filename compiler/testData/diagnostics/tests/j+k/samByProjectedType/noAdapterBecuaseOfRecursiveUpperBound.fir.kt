// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// FIR_DUMP
// FILE: Function.java
public interface Function<E extends CharSequence, F extends java.util.Map<String, E>> {
    E handle(F f);
}

// FILE: A.java
public class A {
    public void foo(Function<?, ?> l) {
    }

    public static void bar(Function<?, ?> l) {
    }
}

// FILE: main.kt
fun main() {
    A().foo {
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }

    A.bar {
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }

    A.bar(<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>Function<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "" })
    A.bar(Function<CharSequence, Map<String, CharSequence>>{ x -> x[""] })
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaFunction, javaType, lambdaLiteral, nullableType, samConversion, starProjection, stringLiteral, typeParameter,
typeWithExtension */
