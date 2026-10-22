// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
@Target(AnnotationTarget.TYPE)
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (a: <expr>@Ann(x)</expr> Int, b) = 1 to 2
}
