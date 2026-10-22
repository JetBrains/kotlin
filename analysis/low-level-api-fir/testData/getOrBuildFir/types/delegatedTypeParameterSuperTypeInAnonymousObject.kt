// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
interface MyInterface

fun <T : MyInterface> foo(i: T) {
    object : <expr>T</expr> by i {}
}
