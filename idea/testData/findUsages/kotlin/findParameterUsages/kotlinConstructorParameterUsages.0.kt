// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetParameter
// OPTIONS: usages
open class A<T>(<caret>foo: T) {
    init {
        println(foo)
    }

    val t: T = foo
}
