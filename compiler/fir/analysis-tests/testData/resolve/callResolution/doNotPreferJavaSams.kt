// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78832
// FILE: JavaTransformer.java

public interface JavaTransformer<OUT, IN> {
    OUT foo(IN i);
}

// FILE: main.kt
fun interface Transformer<OUT, IN> {
    fun foo(i: IN): OUT
}

class MyProvider<T>

fun <T> bar(value: T): MyProvider<T> = null!! // (1)
fun <T> bar(valueTransformer: Transformer<MyProvider<out T>, Any>): MyProvider<T> = null!! // (2)

fun <T> baz(value: T): MyProvider<T> = null!! // (3)
fun <T> baz(valueTransformer: JavaTransformer<MyProvider<out T>, Any>): MyProvider<T> = null!! // (4)

fun boo(hoo: MyProvider<String>) {}

fun foo(x: MyProvider<String>) {
    val barRes = bar { x } // resolved to (2) both in K1 & K2
    boo(barRes)

    val bazRes = baz { x } // resolved to (3) in K2 and to (4) in K1 with +DisableCompatibilityModeForNewInference
    boo(<!ARGUMENT_TYPE_MISMATCH!>bazRes<!>) // Error only in K2
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funInterface, functionDeclaration, interfaceDeclaration,
javaType, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration, samConversion, typeParameter */