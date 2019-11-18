// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): String {
        return "OK"
    }
}

interface B : A

class C : B {
    override fun foo(): String {
        return super.foo()
    }
}

fun box(): String {
    return C().foo()
}
