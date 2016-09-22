// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package pack

open class B(val n: Int) {
    operator fun <caret>invoke(i: Int){}
}

object Obj : B(0)

fun f() = B(1)

fun test() {
    f(1).invoke(2)
    f(2)(2)

    val v = Obj
    v(1)

    listOf(pack.Obj)[0](1)
}
