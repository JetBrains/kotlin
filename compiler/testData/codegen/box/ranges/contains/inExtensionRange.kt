// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

operator fun Int.rangeTo(right: String): ClosedRange<Int> = this..this + 1
operator fun Long.rangeTo(right: Double): ClosedRange<Long> = this..right.toLong() + 1
operator fun String.rangeTo(right: Int): ClosedRange<String> = this..this

fun box(): String {
    assert(0 !in 1.."a")
    assert(1 in 1.."a")

    assert(0L !in 1L..2.0)
    assert(2L in 1L..3.0)

    assert("a" !in "b"..1)
    assert("a" in "a"..1)

    return "OK"
}
