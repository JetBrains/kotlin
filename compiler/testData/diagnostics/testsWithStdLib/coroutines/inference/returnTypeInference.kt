// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class Controller

fun <R> generate(g: suspend Controller.() -> R): R = TODO()

val test1 = generate {
    3
}