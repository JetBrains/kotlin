// RUN_PIPELINE_TILL: FRONTEND
open class A

class E

abstract class Wrapper<T: A>(protected val t: T)

class MyWrapper(a: A): Wrapper<A>(a)

// This wrapper is not legal
class TheirWrapper(e: E): <!INAPPLICABLE_CANDIDATE!>Wrapper<<!UPPER_BOUND_VIOLATED!>E<!>><!>(e)

data class Pair<out T>(val a: T, val b: T)

fun foo(): String {
    val matrix: Pair<Wrapper<*>>
    // It's not legal to do such a thing because E is not derived from A
    // But we should not have assertion errors because of it!
    matrix = Pair(MyWrapper(A()), TheirWrapper(E()))
    return matrix.toString()
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, data, functionDeclaration, localProperty, nullableType, out,
outProjection, primaryConstructor, propertyDeclaration, starProjection, typeConstraint, typeParameter */
