interface TypeParameter

interface A<OptionalTypeParameter : TypeParameter?> {
    fun whatever(params: OptionalTypeParameter)
}

// Class B doesn't use the type parameter
class B : A<Nothing?> {
    override fun whatever(params: Nothing?) {
        // We know this class doesn't use the type parameter, the value is just 'null'
    }
}

inline fun <reified T : TypeParameter?> foo(it: A<T>) = "OK"

fun box(): String {
    val b = B()
    return foo(b)
}