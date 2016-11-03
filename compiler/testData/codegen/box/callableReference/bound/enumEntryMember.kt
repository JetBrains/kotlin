// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

enum class E {
    A, B;

    fun foo() = this.name
}

fun box(): String {
    val f = E.A::foo
    if (f() != "A") return "Fail 1: ${f()}"
    if (f != E.B::foo) return "Fail 2"

    return "OK"
}
