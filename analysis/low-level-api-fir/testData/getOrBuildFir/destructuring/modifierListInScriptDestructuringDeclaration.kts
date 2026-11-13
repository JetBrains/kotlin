// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDeclarationModifierList

data class X(val a: Int, val b: Int)
annotation class Anno

<expr>@Anno</expr>
val (a, b) = X(1, 2)
