// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: usages, constructorUsages
package a

public open class Outer {
    public open class <caret>A {
        public var bar: String = "bar";

        public open fun foo() {

        }

        class object {
            public var bar: String = "bar";

            public open fun foo() {

            }
        }
    }
}