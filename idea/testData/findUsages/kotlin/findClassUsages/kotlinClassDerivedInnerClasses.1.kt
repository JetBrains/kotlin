// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
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