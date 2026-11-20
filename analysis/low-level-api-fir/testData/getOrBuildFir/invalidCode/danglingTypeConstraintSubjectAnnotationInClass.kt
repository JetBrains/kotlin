// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

annotation class Anno(val s: String)

class Foo<T> where <expr>@Anno("str")</expr> AA : Any
