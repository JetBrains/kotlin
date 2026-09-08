// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
// ISSUE: KT-88982

annotation class Anno(val s: String)

const val STRING: String? = null

<expr>@Anno(STRING)</expr>
fun test() {}
