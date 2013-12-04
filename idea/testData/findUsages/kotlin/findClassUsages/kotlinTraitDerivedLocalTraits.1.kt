// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedInterfaces

fun foo() {
    open class B: A() {

    }

    fun bar() {
        trait Z: A {

        }

        trait U: Z {

        }
    }
}