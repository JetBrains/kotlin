// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Either

infix fun <A, B, C> ((A) -> B).andThen(g: (B) -> C): (A) -> C = TODO()

fun unsafeRunAsync(cb: (Either) -> Unit) {}

fun runAsync(cb: (Either) -> Unit) {
    unsafeRunAsync(cb.andThen { })
}