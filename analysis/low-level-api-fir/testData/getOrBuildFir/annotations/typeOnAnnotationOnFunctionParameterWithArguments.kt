// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

fun t(addCommaWarning: <expr>@Anno("abcd")</expr> Boolean) {

}

open class A

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)