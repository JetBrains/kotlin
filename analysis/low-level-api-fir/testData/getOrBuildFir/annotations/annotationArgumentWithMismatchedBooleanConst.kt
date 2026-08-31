// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
// ISSUE: KT-88982

annotation class Anno(val b: Boolean)

const val BOOLEAN: Boolean = 1

<expr>@Anno(BOOLEAN)</expr>
fun test() {}
