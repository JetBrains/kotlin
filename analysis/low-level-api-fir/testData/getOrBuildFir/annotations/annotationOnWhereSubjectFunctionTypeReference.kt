// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference

annotation class Anno(val s: String)

fun <T, U> foo(t: T, u: U) where @<expr>Anno</expr>("str") U : Any {}
