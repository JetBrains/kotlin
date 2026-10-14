// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
fun foo(p: <expr>@Anno("str") Int</expr>) {}

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)
