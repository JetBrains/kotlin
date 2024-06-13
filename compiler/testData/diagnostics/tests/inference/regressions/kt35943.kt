// DIAGNOSTICS: -UNUSED_EXPRESSION

class Inv<I>
fun <T> create(): Inv<T> = TODO()

fun main() {
    <!TYPE_MISMATCH!>if (true) create() else null<!>
}
