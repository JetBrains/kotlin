// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87640

fun bar() = foo<!NO_VALUE_FOR_PARAMETER!>()<!>
fun foo(arg: Int) = arg

class A {
    fun foo(arg: Int) = arg
}

fun getA() = A()

fun qualified() = getA().foo<!NO_VALUE_FOR_PARAMETER!>()<!>
fun safeCall(a: A?) = a?.foo<!NO_VALUE_FOR_PARAMETER!>()<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall */
