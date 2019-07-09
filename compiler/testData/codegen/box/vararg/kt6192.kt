fun barB(vararg args: Byte) = args
fun barC(vararg args: Char) = args
fun barD(vararg args: Double) = args
fun barF(vararg args: Float) = args
fun barI(vararg args: Int) = args
fun barJ(vararg args: Long) = args
fun barS(vararg args: Short) = args
fun barZ(vararg args: Boolean) = args

fun sumInt(x: Int, vararg args: Int): Int {
    var result = x
    for(a in args) {
        result += a
    }
    return result
}

fun sumFunOnParameters(x: Int, vararg args: Int, f: (Int) -> Int): Int {
    var result = f(x)
    for(a in args) {
        result += f(a)
    }
    return result
}

fun concatParameters(vararg args: Int): String {
    var result = ""
    for(a in args) {
        result += a.toString()
    }
    return result
}

fun box(): String {

    val aB = ByteArray(3)
    val aC = CharArray(3)
    val aD = DoubleArray(3)
    val aF = FloatArray(3)
    val aI = IntArray(3)
    aI[0] = 1
    aI[1] = 2
    aI[2] = 3
    val bI = IntArray(2)
    bI[0] = 4
    bI[1] = 5
    val aJ = LongArray(3)
    val aS = ShortArray(3)
    val aZ = BooleanArray(3)


    if (barB(*aB, 23.toByte()).size != 4) return "fail: Byte"
    if (barB(11.toByte(), *aB, 23.toByte(), *aB).size != 8) return "fail: Byte"

    if (barC(*aC, 'A').size != 4) return "fail: Char"
    if (barC('A', *aC, 'A', *aC).size != 8) return "fail: Char"

    if (barD(*aD, 2.3).size != 4) return "fail: Double"
    if (barD(*aD, *aD, 2.3).size != 7) return "fail: Double"

    if (barF(*aF, 2.3f).size != 4) return "fail: Float"
    if (barF(*aF, 2.3f, 1.1f).size != 5) return "fail: Float"

    if (barI(*aI, 23).size != 4) return "fail: Int"
    if (barI(11, 10, *aI, 23).size != 6) return "fail: Int"
    if (barI(100, *aI, *aI).size != 7) return "fail: Int 3"

    if (sumInt(100, *aI) != 106) return "fail: sumInt 1"
    if (sumInt(100, *aI, 200) != 306) return "fail: sumInt 2"
    if (sumInt(100, *aI, *aI) != 112) return "fail: sumInt 3"
    if (sumFunOnParameters(100, *aI, 200) { 2*it } != 612) return "fail: sumFunOnParameters 1"
    if (sumFunOnParameters(100, *aI, *aI) { 2*it } != 224) return "fail: sumFunOnParameters 2"

    if (concatParameters(1,2,3) != "123") return "fail: concatParameters 1"
    if (concatParameters(*aI) != "123") return "fail: concatParameters 2"
    if (concatParameters(4, 5, *aI) != "45123") return "fail: concatParameters 3"
    if (concatParameters(*aI, 4, 5) != "12345") return "fail: concatParameters 4"
    if (concatParameters(*aI, *bI) != "12345") return "fail: concatParameters 5"
    if (concatParameters(*aI, 7, 8, *bI) != "1237845") return "fail: concatParameters 6"
    if (concatParameters(*aI, 7, *bI, *aI, 9) != "1237451239") return "fail: concatParameters 7"

    if (barJ(*aJ, 23L).size != 4) return "fail: Long"
    if (barJ(*aJ, 23L, *aJ, *aJ).size != 10) return "fail: Long"

    if (barS(*aS, 23.toShort()).size != 4) return "fail: Short"
    if (barS(*aS, *aS, 23.toShort(), *aS).size != 10) return "fail: Short"

    if (barZ(*aZ, true).size != 4) return "fail: Boolean"
    if (barZ(false, *aZ, true, *aZ).size != 8) return "fail: Boolean"

    return "OK"
}
