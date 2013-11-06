// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetParameter
// OPTIONS: usages
open class A<T>(<caret>foo: T) {
    {
        println(foo)
    }

    val t: T = foo
}