// IGNORE_BACKEND: ANDROID
// ISSUE: KT-60370

data object A {
    fun copy() = "O"
}

data object B {
    fun copy(test: String) = test
}

fun box(): String {
    return A.copy() + B.copy("K")
}
