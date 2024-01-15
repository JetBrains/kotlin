// FIR_IDENTICAL
// ISSUE: KT-64891

val b: Int.() -> Int = { 10 }
val Int.b: () -> String get() = { "B" }

fun main() {
    5.(b)().inv() // should be Int
    5.b().length  // should be String
}

fun <T> id(it: T) = it

fun rain() {
    5.(b)().inv()
    5.(id(b))().inv() // should be consistent
}
