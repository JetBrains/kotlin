// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

class Outer<A> {
    fun <B> func(): String {
        class L {
            fun foo(): String = "OK"
        }
        return (L::foo)(L())
    }
}

fun box(): String {
    return Outer<Int>().func<Char>()
}
