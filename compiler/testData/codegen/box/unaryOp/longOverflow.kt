// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature; for reasons this test is ignored, go to KT-46419

fun box(): String {
    val a: Long = -(1 shl 31)
    if (a != -2147483648L) return "fail: in this case we should add to ints and than cast the result to long - overflow expected"
    return "OK"
}
