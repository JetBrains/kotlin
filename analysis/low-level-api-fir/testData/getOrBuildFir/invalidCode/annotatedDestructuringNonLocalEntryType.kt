// KT-74654
// KT-89821: annotations on destructuring entries have no FIR yet
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (a, @<expr>Ann</expr>(x) b) = 1 to 2
}
