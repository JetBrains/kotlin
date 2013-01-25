enum class A() {
    ENTRY: A(){ override fun t() = "OK"}

    abstract fun t(): String
}

fun f(a: A) = a.t()

fun box()= f(A.ENTRY)
