// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
annotation class Ann(val s: String)

const val x = "str"

val (<expr>@Ann(x)</expr> a, b) = 1 to 2
