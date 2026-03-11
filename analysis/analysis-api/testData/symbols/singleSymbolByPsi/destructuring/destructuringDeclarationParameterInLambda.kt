// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtParameter

data class X(val a: Int, val b: Int)

fun x(action: (X, Int) -> Unit) {}

fun main() {
    x { <expr>(a, b)</expr>, i ->

    }
}
