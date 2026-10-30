// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

annotation class Anno(val s: String)

val <T> List<T>.prop: Int where <expr>@Anno("str")</expr> AA : Any get() = 1
