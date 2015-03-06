class A() {
    default object {
        val value = 10
    }
}

fun box() = if (A.value == 10) "OK" else "Fail ${A.value}"
