// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtStringTemplateExpression

class ResolveMe(
    addCommaWarning: @Anno(<expr>"abc"</expr>) Boolean = false
)

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)