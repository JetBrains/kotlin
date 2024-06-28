// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

fun check(): <expr>@Anno("ab")</expr> Int = 1

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)