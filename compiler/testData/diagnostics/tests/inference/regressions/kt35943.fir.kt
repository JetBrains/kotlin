// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION

class Inv<I>
fun <T> create(): Inv<T> = TODO()

fun main() {
    if (true) create() else null
}
