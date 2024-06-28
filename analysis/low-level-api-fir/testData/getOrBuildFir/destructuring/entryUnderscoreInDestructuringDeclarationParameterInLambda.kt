// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

data class X(val a: Int, val b: Int)

fun x(action: (X) -> Unit) {}

fun main() {
    x { (<expr>_</expr>, b) ->

    }
}