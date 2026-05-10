// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

class Callbacks {
    companion object {
        inline operator fun of(first: () -> Unit, vararg other: () -> Unit): Callbacks {
            return Callbacks()
        }
    }
}

fun takeC(c: Callbacks, a: Any) = Unit

fun main() {
    var a: Any?
    a = 42
    takeC([{ a = null }], <expr>a</expr>)
}
