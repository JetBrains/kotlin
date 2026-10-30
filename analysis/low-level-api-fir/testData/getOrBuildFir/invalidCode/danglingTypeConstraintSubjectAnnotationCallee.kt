// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression

annotation class Anno(val s: String)

fun <T> foo(t: T) where @<expr>Anno</expr>("str") AA : Any {}
