// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

fun <expr>@receiver:Anno("ab")</expr> Int.check() = 1

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)