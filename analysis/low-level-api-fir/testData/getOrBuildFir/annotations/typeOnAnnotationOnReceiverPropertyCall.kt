// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtTypeReference

val @<expr>Anno</expr>("a") Int.i: String get() = ""

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)