// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_DUMP
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-51404

class In<in T>

interface Base
interface Derived : Base

fun <K1 : Derived> In<K1>.foo(fn: (K1) -> Int): K1 = TODO()

@JvmName("foo2")
fun <K2> In<K2>.foo(fn: (K2) -> CharSequence): K2 = TODO()

fun main(x: In<Base>) {
    // In<Base> <: In<K1> => K1 <: Base
    // fix K1 := Base & Derived = Derived
    // fix K2 = Base
    // => different input types => impossible to run ELA
    x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> {
        ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, in,
interfaceDeclaration, lambdaLiteral, nullableType, stringLiteral, typeConstraint, typeParameter */
