// FIR_IDENTICAL
// ISSUE: KT-51003

interface Assert<T>

fun <T> createAssert(value: T): Assert<T> = null!!

fun <A, B : Comparable<A>> Assert<B>.isGreaterThanOrEqualTo(other: A) {}

fun test_1(long: Long) {
    // FE 1.0: OK
    // FIR: ARGUMENT_TYPE_MISMATCH
    createAssert(long).isGreaterThanOrEqualTo(10 * 10)
}

fun getNullableLong(): Long? = null
fun takeLong(x: Long) {}

fun test_2() {
    val x = getNullableLong() ?: 10 * 60
    takeLong(x)
    // FE 1.0 infers type of `x` to `Long`
    // FIR infers it to `Number` as `CST(Long, Int)`
}
