// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM, JS

class MyClass {
    companion object {
        const val token = "token"
    }

    val token by lazy { MyClass.token }
}
