// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    trait Z: A {

    }

    fun doSomething(x: A, y: A) {

    }

    doSomething(object : A() {}, object: Z {})

    fun bar() {
        val x = object: Z {

        }
    }
}