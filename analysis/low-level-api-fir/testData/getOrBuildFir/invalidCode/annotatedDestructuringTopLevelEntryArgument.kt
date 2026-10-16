// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
annotation class Ann(val s: String)

const val x = "str"

@Ann(x)
val (a, @Ann(<expr>x</expr>) b) = 1 to 2
