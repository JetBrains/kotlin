// IGNORE_BACKEND_FIR: JVM_IR
interface T1 {
    fun foo() = "O"
}

interface T2 {
    fun foo() = "K"
}

class A : T1, T2 {
    override fun foo() = super<T1>.foo() + super<T2>.foo()
}

fun box() = A().foo()
