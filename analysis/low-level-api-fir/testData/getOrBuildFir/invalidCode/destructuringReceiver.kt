// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
data class P(val a: Int, val b: Int)

fun foo() {
    val <expr>Int</expr>.(a, b) = P(1, 2)
}
