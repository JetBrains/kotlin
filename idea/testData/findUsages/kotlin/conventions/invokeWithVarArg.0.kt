// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package pack

class P {
    operator fun <caret>invoke(vararg i: Int) = 1
}

fun f(p: P) {
    p()
    p(1, 2)
    p(1)
    p.invoke()
    p.invoke(1, 2, 3)
}