fun check(expected: ByteArray, actual: ByteArray) {
    if (actual !is ByteArray) throw Error("!is")
    if (actual.size != expected.size) throw Error("size")
    for (i in 0..expected.size - 1) {
        if (actual[i] != expected[i]) throw Error("[$i]")
    }
}

fun box(): String {

    val bA = ByteArray(1)
//    check(bA)

    if (bA !is ByteArray) return "fail 1"
    bA[0] = 1.toByte()

    val bAc = bA.copyOf()
    if (bAc !is ByteArray) return "fail 2"
    if (bAc[0] != bA[0]) return "fail 3"
    if (bAc.size != 1) return "fail 4"

    val bAcs = bA.copyOf(2)
    if (bAcs !is ByteArray) return "fail 5"
    if (bAcs[0] != bA[0]) return "fail 6"
    if (bAcs[1] != 0.toByte()) return "fail 7"
    if (bAcs.size != 2) return "fail 8"

    val bAcs2 = bAcs.copyOf(1)
    if (bAcs2 !is ByteArray) return "fail 9"
    if (bAcs2[0] != 1.toByte()) return "fail 10"


//    js("Int32Array.from([1, 2, 3])")
//    js("Int32Array.of(1, 2, 3)")
//    js("(new Int32Array(10)).set([1, 2, 3], 1)")



    return "OK"
}