// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
// ISSUE: KT-88982

annotation class Anno(val c: Char)

const val CHAR: Char = 65

<expr>@Anno(CHAR)</expr>
fun test() {}
