// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedInterfaces

fun foo() {
    open class B: A() {

    }

    fun bar() {
        interface Z: A {

        }

        interface U: Z {

        }
    }
}
