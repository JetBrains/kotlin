// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class A(val b: String) {
    E1("OK"){ override fun t() = b };

    abstract fun t(): String
}

fun box()= A.E1.t()
