// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-51400
// LANGUAGE: +EagerLambdaAnalysis +CallCompletionRefinementsFor25
// FIR_DUMP
import kotlin.reflect.KFunction0

fun bar(): Inv<KFunction0<Int>> = TODO()

fun bar2(): Int = 1
fun bar2(x: Int): Int = 1

class Inv<P>(x: P)

fun <K, L: Inv<out M>, M> K.singleOverload(cr1: M, cr2: (L) -> K, cr3: () -> L, fn: (K) -> Int): K = TODO()

@JvmName("debounce1")
fun <K, L: Inv<out M>, M> K.debounce(cr1: M, cr2: (L) -> K, cr3: () -> L, fn: (K) -> Int): K = TODO()

@JvmName("debounce2")
fun <K, L: Inv<out M>, M> K.debounce(cr1: M, cr2: (L) -> K, cr3: () -> L, fn: (K) -> String): K = TODO()

fun foo(x: Inv<KFunction0<Int>>): Int = 1

class A {
    fun foo(x: Inv<KFunction0<Int?>>): Number = 2
    @JvmName("foo2")
    fun foo(x: Inv<KFunction0<Int>>): Number = 3

    fun main(i: Int) {
        // L <: Inv<out M>
        // Int <: K
        // Inv<KFunction0<kotlin/Int>> <: L from ::bar
        // KFunction0<kotlin/Int> <: M
        // KFunction0<TypeVariable(_R)> <: M ::bar2
        // L == Inv<KFunction0<kotlin/Int>>
        // Number <: K from ::foo
        // K == Number
        val x1 = i.singleOverload(::bar2, ::foo, ::bar) { value -> 0 }
        // Here it's mostly the same, but can we fix K while it's used as a return type?
        val x2 = i.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!>(::bar2, ::foo, ::bar) { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>value<!> -> 0 }

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x1<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: debounce, [/debounce, /debounce]")!>x2<!>
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, funWithExtensionReceiver,
functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
