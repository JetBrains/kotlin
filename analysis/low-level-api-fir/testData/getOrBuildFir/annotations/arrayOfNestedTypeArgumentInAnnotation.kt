// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
// ISSUE: KT-81110

@Anno(arrayOf<List<<expr>String</expr>>>())
fun foo() {}

annotation class Anno(val a: Array<List<String>>)
