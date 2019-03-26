// Even before any IR lowerings, the type of `when` is determined to be
// Unit even though the outer `if` still returns `Int?`. This results
// in a ClassCastException when that Unit is converted into a Number.
// IGNORE_BACKEND: JVM_IR
fun test(
        b: Boolean,
        i: Int
) {
    if (b) {
        when (i) {
            0 -> foo(1)
            else -> null
        }
    } else null
}

fun foo(i: Int) = i

fun box(): String {
    test(true, 1)
    return "OK"
}