interface TypeParameter

interface A<OptionalTypeParameter : TypeParameter?> {
    fun whatever(params: OptionalTypeParameter)
}

class B : A<Nothing?> {
    override fun whatever(params: Nothing?) {
        // We know this class doesn't use the type parameter, the value is just 'null'
    }
}

inline fun <reified T : TypeParameter?> foo(it: A<T>) = Unit

fun main() {
    val b = B()

    foo(b)
}
