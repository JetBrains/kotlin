// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
package a

public open class Outer {
    public open class <caret>A {
        public var bar: String = "bar";

        public open fun foo() {

        }

        companion object {
            public var bar: String = "bar";

            public open fun foo() {

            }
        }
    }
}
