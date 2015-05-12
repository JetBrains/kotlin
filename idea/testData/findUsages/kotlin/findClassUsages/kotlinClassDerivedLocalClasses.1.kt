// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    open class B: A() {

    }

    open class C: Y {

    }

    fun bar() {
        interface Z: A {

        }

        interface U: Z {

        }
    }
}
