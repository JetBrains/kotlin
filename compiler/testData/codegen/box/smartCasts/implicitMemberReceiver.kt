// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open val a = "OK"
}

class B : A() {
    override val a = "FAIL"
    fun foo() = "CRUSH"
}

class C {
    fun A?.complex(): String {
        if (this is B) return foo()
        else if (this != null) return a
        else return "???"
    }

    fun bar() = A().complex()
}

fun box() = C().bar()
