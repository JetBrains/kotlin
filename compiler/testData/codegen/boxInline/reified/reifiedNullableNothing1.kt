// LANGUAGE: +NullableNothingInReifiedPosition
// ISSUE: KT-54227
// IGNORE_BACKEND_K1: ANY

// FILE: lib.kt
interface TypeParameter

inline fun <reified T : TypeParameter?> foo(it: A<T>) = "OK"

interface A<OptionalTypeParameter : TypeParameter?> {
    fun whatever(params: OptionalTypeParameter)
}

// FILE: main.kt
// Class B doesn't use the type parameter
class B : A<Nothing?> {
    override fun whatever(params: Nothing?) {
        // We know this class doesn't use the type parameter, the value is just 'null'
    }
}

fun box(): String {
    val b = B()
    return foo(b)
}
