// TODO investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class A {
    companion object {
        fun ok() = "OK"
    }
}

fun box() = (A.Companion::ok)()