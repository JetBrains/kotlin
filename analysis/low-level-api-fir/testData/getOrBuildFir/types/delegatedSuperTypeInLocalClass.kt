// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
interface MyInterface

fun foo(i: MyInterface) {
    class Local : <expr>MyInterface</expr> by i
}
