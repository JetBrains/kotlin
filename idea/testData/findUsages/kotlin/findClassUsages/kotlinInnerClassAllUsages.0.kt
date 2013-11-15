// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: usages, constructorUsages
package a

public open class Outer {
    public open inner class <caret>A {
        public var bar: String = "bar";

        public open fun foo() {

        }
    }
}