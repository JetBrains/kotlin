// !DIAGNOSTICS: -UNUSED_PARAMETER
public inline fun <reified T> Array(n: Int, block: (Int) -> T): Array<T> = null!!

// KT-312 Nullability problem when a nullable version of a generic type is returned

fun <T> Array<out T>.safeGet(index : Int) : T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!> {
    return if (index < size()) this[index] else null
}

val args : Array<String> = Array<String>(1, {""})
val name : String = <!TYPE_MISMATCH!>args.safeGet<String>(0)<!> // No error, must be type mismatch
val name1 : String? = args.safeGet(0)

