// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclaration
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

data class X(val a: Int, val b: Int)

fun x(action: (X) -> Unit) {}

fun main() {
    x { <expr>(a, b)</expr> ->

    }
}