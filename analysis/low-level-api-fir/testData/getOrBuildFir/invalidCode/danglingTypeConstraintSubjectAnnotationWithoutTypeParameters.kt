// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

annotation class Anno(val s: String)

fun foo() where <expr>@Anno("str")</expr> AA : Any {}
