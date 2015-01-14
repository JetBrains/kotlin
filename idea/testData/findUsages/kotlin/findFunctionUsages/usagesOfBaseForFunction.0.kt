// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages, skipImports

trait A {
    fun foo()
}

class B: A {
    override fun <caret>foo() {} // Find usages gives no results
}

fun main(a: A) {
    a.foo()
}

// for KT-3769 Find usages gives no result for overrides
