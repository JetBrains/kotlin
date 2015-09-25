// SUGGESTED_NAMES: i, getA
// PARAM_DESCRIPTOR: value-parameter val t: T defined in test
// PARAM_TYPES: T
fun foo<U>(u: U) = 1

fun test<T>(t: T) {
    val a = <selection>foo<T>(t)</selection>
}