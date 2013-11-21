// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetParameter
// OPTIONS: usages
package server;

open class A<T>(open var <caret>foo: T)

open class B: A<String>("") {
    override var foo: String
        get() {
            println("get")
            return ""
        }
        set(value: String) {
            println("set:" + value)
        }
}