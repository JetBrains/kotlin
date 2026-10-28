// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

annotation class Anno(val s: String)

fun <T, U> foo(t: T, u: U) where <expr>@Anno("str")</expr> U : Any {}
