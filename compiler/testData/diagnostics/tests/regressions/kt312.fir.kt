// !WITH_NEW_INFERENCE
// KT-312 Nullability problem when a nullable version of a generic type is returned

fun <T> Array<out T>.safeGet(index : Int) : T? {
    return if (index < size) this[index] else null
}

val args : Array<String> = Array<String>(1, {""})
val name : String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>args.safeGet<String>(0)<!> // No error, must be type mismatch
val name1 : String? = args.safeGet(0)
