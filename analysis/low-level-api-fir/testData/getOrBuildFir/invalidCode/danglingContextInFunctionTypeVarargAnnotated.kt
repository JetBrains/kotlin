// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
@Target(AnnotationTarget.TYPE)
annotation class Anno

fun foo(vararg p: @Anno context(<expr>Int</expr>)) {}
