// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
annotation class Ann(val s: String)

const val x = "str"

class C {
    val [a, <expr>@Ann(x)</expr> b] = 1 to 2
}
