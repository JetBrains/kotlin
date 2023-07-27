// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
// IGNORE_FIR

class ResolveMe(
    var addCommaWarning: <expr>@Anno("abc")</expr> Boolean = false,
) : A() {

}

open class A

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)