// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_COMPARISON

private val localObject = object : Any() {
    fun <caret>f() {
    }
}

class Foo {
    fun bar() {
        localObject.f()
    }
}