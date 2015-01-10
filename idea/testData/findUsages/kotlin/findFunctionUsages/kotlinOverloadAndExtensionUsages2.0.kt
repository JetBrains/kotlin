// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: overloadUsages
trait X {

}

class A: X {

}

object O: A() {
    fun <caret>foo() {

    }
}

fun A.foo(s: String) {

}

fun X.foo(n: Int) {

}
