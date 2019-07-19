interface A {
    private inline fun <reified T> callDefault(b: () -> String): String {
        "String" is T
        return b()
    }

    fun ok() = callDefault<String> { "OK" }
}

class B : A

fun box(): String {
    return B().ok()
}

// 0 INVOKESTATIC A$DefaultImpls.callDefault