// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package pack

class P
operator fun P.<caret>invoke() = 1

fun f(p: P) {
    p()
    p.invoke()
}