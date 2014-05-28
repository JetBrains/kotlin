open class B {
    fun getX() = 1
}

class C : B() {
    val x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1
}