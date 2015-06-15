class A : Test<String>() {
    fun foo(): String? = value
}

fun box(): String {
    return if (A().foo() == null) "OK" else "Fail"
}
