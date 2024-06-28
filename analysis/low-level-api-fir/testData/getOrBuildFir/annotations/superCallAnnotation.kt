// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

@Target(AnnotationTarget.TYPE)
annotation class Anno

open class A

class B : <expr>@Anno</expr> A()