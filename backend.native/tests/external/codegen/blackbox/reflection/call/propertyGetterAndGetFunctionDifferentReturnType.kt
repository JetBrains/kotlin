// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

data class Foo(val id: String) {
    fun getId() = -42 // Fail
}

fun box(): String {
    return Foo::id.call(Foo("OK"))
}
