// WITH_STDLIB

class MyClass {
    companion object {
        const val token = "token"
    }

    val token by lazy { MyClass.token }
}
