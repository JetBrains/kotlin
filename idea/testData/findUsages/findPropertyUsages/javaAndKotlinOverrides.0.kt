// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: overrides
open class A<T> {
    open var <caret>foo: T
}

open class B: A<String>() {
    override var foo: String
        get() {
            println("get")
            return super<A>.foo
        }
        set(value: String) {
            println("set:" + value)
            super<A>.foo = value
        }

    fun baz(a: A<String>) {
        a.foo = ""
        println(a.foo)
    }
}
