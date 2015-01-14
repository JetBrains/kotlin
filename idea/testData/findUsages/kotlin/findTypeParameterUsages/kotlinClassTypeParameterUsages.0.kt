// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetTypeParameter
// OPTIONS: usages
open class A<<caret>T>(foo: T, list: List<T>) {
    {
        fun T.bar() {}

        foo.bar()
    }

    val t: T = foo
    fun bar(t: T): T = t
}
