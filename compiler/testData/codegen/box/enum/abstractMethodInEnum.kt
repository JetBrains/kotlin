// IGNORE_BACKEND_FIR: JVM_IR
enum class A() {
    ENTRY(){ override fun t() = "OK"};

    abstract fun t(): String
}

fun f(a: A) = a.t()

fun box()= f(A.ENTRY)
