// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
class Outer {
    open class B: A() {

    }

    open class C: Y {

    }

    class Inner {
        trait Z: A {

        }

        class U: Z {

        }
    }
}
