// KT-74654
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
annotation class Ann(val s: String)

const val x = "str"

<expr>@Ann(x)</expr>
val (a, b) = 1 to 2
