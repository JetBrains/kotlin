// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

annotation class Anno(val s: String)

val <T, U> Map<T, U>.prop: Int where <expr>@Anno("str")</expr> U : Any get() = 1
