// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
class C

operator fun C.plusAssign(p: Int) = this

fun foo() {
    val <caret>c = C()
    c += 10
}

// ERROR: 'operator' modifier is inapplicable on this function: must return Unit
// ERROR: Function 'plusAssign' should return Unit to be used by corresponding operator '+='
