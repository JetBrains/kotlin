// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +ProperTypeInferenceConstraintsProcessing

fun main(args: Array<String>) {
    val zero = coerce<Int, String>(0)
}

fun <T, U> coerce(t: T): U {
    // Should be an error somewhere because this code leads to unsoundness
    // We may report that `Constrain<U, *, in T>?` type definition is unsound or the call `Bind(constrain)`
    // See KT-50230
    val constrain: Constrain<U, *, in T>? = null
    val bind = Bind(constrain)
    return bind.upcast(t)
}

class Constrain<A, B : A, C : B>

class Bind<A, B : A, C : B>(val constrain: Constrain<A, B, C>?) {
    fun upcast(c: C): A = c
}
