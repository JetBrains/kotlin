// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

val p: context(<expr>@Anno("1" + "2")</expr> Int)
