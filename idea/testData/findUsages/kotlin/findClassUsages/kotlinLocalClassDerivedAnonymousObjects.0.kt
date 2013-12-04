// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses
fun foo() {
    open class <caret>A

    val b = object: A() {}

    trait T: A

    fun bar() {
        val c = object: A() {}

        val d = object: T {}
    }
}

