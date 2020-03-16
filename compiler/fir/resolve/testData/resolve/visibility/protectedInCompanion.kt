abstract class A {
    companion object {
        protected val PROTECTED_CONST: String = ""
    }
}

class B : A() {
    val y: String = PROTECTED_CONST
}
