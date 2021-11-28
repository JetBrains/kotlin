abstract class A {
    companion object {
        protected val PROTECTED_CONST: String = ""
    }
}

class B : A() {
    val y: String = <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC!>PROTECTED_CONST<!>
}
