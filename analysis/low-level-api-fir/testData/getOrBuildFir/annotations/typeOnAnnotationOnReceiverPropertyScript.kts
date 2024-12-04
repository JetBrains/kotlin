// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

val <expr>@Anno("a")</expr> Int.i: String get() = ""

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)