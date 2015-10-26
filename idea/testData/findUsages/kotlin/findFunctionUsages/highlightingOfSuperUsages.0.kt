// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages, skipImports
// HIGHLIGHTING

open class A : I {
    open fun foo() {}
}

class B: A() {
    override fun <caret>foo() {}
}

fun test(i: I) {
    i.foo()
    A().foo()
    B().foo()
}
