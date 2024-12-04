// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtTypeReference
open class A

class B(val prop: String = "${foo()}"): <expr>A</expr>()

fun foo() = 42