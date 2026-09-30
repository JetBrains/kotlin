// KT-74654
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
annotation class Ann(val s: String)

const val x = "str"

class C {
    @<expr>Ann</expr>(x)
    val (a, b) = 1 to 2
}
