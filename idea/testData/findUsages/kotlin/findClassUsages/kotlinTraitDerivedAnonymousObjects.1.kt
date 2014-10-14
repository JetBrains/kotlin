// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    fun doSomething(a: X, b: X) {

    }

    doSomething(object : A() {}, object: X {})

    fun bar() {
        val x = object : X {

        }
    }
}