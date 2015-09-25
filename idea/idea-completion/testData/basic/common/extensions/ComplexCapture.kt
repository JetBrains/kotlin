interface I<T>

fun <E, T : I<E>> T.ext() : T = this

class A<T> : I<T>

fun main(args: Array<String>) {
    val a = A<Int>()
    a.<caret>
}

// EXIST: ext