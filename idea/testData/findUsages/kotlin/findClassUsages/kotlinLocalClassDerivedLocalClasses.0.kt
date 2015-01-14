// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
fun foo() {
    open class <caret>A

    class B: A()

    trait T: A

    fun bar() {
        class C: A()

        class D: T
    }
}

