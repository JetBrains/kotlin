// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

abstract class Your {
    abstract val your: String

    fun foo() = your
}

val my: String = "O"
    get() = object: Your() {
        override val your = field
    }.foo() + "K"

fun box() = my