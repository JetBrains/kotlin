// TARGET_BACKEND: JVM
// WITH_REFLECT

data class Foo(val id: String) {
    fun getId() = -42 // Fail
}

fun box(): String {
    return Foo::id.call(Foo("OK"))
}
