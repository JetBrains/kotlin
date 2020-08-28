// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// CHECK_SUPER_METHODS_YES_NO_DIALOG: no
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

// DISABLE-ERRORS