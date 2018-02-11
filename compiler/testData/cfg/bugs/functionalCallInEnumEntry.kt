enum class Test(f: () -> Unit) {
    A(getFunc())
}

fun getFunc(): () -> Unit = {}