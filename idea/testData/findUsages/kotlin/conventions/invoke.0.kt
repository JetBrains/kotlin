// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package pack

open class B(val n: Int) {
    open operator fun <caret>invoke(i: Int){}
}

object Obj : B(0)

fun f() = B(1)

fun test() {
    f().invoke(2)
    f()(2)

    val v = Obj
    v(1)

    listOf(Obj)[0](1)
}

class C(): B(12) {
    override fun invoke(i: Int) {}
}

fun cTest(c: C) {
    c(5)
}
