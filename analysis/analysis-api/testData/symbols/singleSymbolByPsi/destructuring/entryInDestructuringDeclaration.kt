// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

data class X(val a: Int, val b: Int)

fun main(x: X) {
    val (<expr>a</expr>, b) = x
}