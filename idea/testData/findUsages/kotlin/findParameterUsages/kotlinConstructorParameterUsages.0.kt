// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
open class A<T>(<caret>foo: T) {
    init {
        println(foo)
    }

    val t: T = foo

    fun usage() {
        return A(foo = ":)")
    }
}

fun usage() {
    A(foo = ":)")
}

// DISABLE-ERRORS