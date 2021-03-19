// FIR_COMPARISON
class Cn<T>

fun <T, C: Cn<T>> C.some(arg: (T) -> Unit): C {
    return this
}

fun main(args: Array<String>) {
    val x = Cn<String>()
    x.<caret>
}

//ELEMENT: some
