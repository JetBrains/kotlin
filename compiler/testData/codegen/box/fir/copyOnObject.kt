// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-60370
// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82378

data object A {
    fun copy() = "O"
}

data object B {
    fun copy(test: String) = test
}

fun box(): String {
    return A.copy() + B.copy("K")
}
