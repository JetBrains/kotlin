// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
// ISSUE: KT-88982

annotation class Anno(val i: Int)

const val INT: Int = 'A'

<expr>@Anno(INT)</expr>
fun test() {}
