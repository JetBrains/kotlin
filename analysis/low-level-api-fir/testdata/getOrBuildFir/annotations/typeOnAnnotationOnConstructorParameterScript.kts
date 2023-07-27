// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
// IGNORE_FIR

class ResolveMe(
    addCommaWarning: <expr>@Anno</expr> Boolean = false
) : A() {

}

open class A

@Target(AnnotationTarget.TYPE)
annotation class Anno