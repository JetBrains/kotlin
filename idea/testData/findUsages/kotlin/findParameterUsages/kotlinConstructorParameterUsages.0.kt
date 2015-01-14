// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetParameter
// OPTIONS: usages
open class A<T>(<caret>foo: T) {
    {
        println(foo)
    }

    val t: T = foo
}
