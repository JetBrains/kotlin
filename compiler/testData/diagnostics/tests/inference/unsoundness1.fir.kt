// SKIP_TXT
// !LANGUAGE: +ProperTypeInferenceConstraintsProcessing

class A<T, F : T>
fun foo(a: A<*, in CharSequence>) {}
fun <T, U> coerce(t: T): U {
    val constrain: Constrain<U, *, in T>? = null
    val bind = Bind(constrain)
    return bind.upcast(t)
}

class Constrain<A, B : A, C : B>

class Bind<A, B : A, C : B>(val constrain: Constrain<A, B, C>?) {
    fun upcast(c: C): A = c
}

fun <T, U> coerce2(t: T): U {
    // We might report an error on unsound type reference Constrain<U, *, T>?, too
    val constrain: Constrain<U, *, T>? = null
    val bind = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>Bind<!>(<!ARGUMENT_TYPE_MISMATCH!>constrain<!>) // WARNING: Type mismatch: inferred type is T but U was expected
    return bind.upcast(t)
}
