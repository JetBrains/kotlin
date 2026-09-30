// KT-74654
// KT-89821: annotations on destructuring entries have no FIR yet
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (<expr>@Ann(x)</expr> a: Int, b: String) = 1 to ""
}
