// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME
import kotlin.experimental.*

fun box(): String {
    // D = 1101 C = 1100
    // 6 = 0110 5 = 0101
    var iarg1: Int = 0xDC56DC56.toInt()
    var iarg2: Int = 0x65DC65DC
    var i1 = iarg1 and iarg2
    var i2 = iarg1 or  iarg2
    var i3 = iarg1 xor iarg2
    var i4 = iarg1.inv()
    var i5 = iarg1 shl 16
    var i6 = iarg1 shr 16
    var i7 = iarg1 ushr 16

    if (i1 != 0x44544454.toInt()) return "fail: Int.and"
    if (i2 != 0xFDDEFDDE.toInt()) return "fail: Int.or"
    if (i3 != 0xB98AB98A.toInt()) return "fail: Int.xor"
    if (i4 != 0x23A923A9.toInt()) return "fail: Int.inv"
    if (i5 != 0xDC560000.toInt()) return "fail: Int.shl"
    if (i6 != 0xFFFFDC56.toInt()) return "fail: Int.shr"
    if (i7 != 0x0000DC56.toInt()) return "fail: Int.ushr"


    // TODO: Use long hex constants after KT-4749 is fixed
    var larg1: Long = (0xDC56DC56L shl 32) + 0xDC56DC56 // !!!!
    var larg2: Long = 0x65DC65DC65DC65DC
    var l1 = larg1 and larg2
    var l2 = larg1 or  larg2
    var l3 = larg1 xor larg2
    var l4 = larg1.inv()
    var l5 = larg1 shl 32
    var l6 = larg1 shr 32
    var l7 = larg1 ushr 32
    
    if (l1 != 0x4454445444544454) return "fail: Long.and"
    if (l2 != (0xFDDEFDDEL shl 32) + 0xFDDEFDDE) return "fail: Long.or"
    if (l3 != (0xB98AB98AL shl 32) + 0xB98AB98A) return "fail: Long.xor"
    if (l4 != 0x23A923A923A923A9) return "fail: Long.inv"
    if (l5 != (0xDC56DC56L shl 32)/*!!!*/) return "fail: Long.shl"
    if (l6 != (0xFFFFFFFFL shl 32) + 0xDC56DC56) return "fail: Long.shr"
    if (l7 != (0x00000000L shl 32) + 0xDC56DC56.toLong()) return "fail: Long.ushr"

    var sarg1: Short = 0xDC56.toShort()
    var sarg2: Short = 0x65DC.toShort()
    var s1 = sarg1 and sarg2
    var s2 = sarg1 or  sarg2
    var s3 = sarg1 xor sarg2
    var s4 = sarg1.inv()
    
    if (s1 != 0x4454.toShort()) return "fail: Short.and"
    if (s2 != 0xFDDE.toShort()) return "fail: Short.or"
    if (s3 != 0xB98A.toShort()) return "fail: Short.xor"
    if (s4 != 0x23A9.toShort()) return "fail: Short.inv"

    var barg1: Byte = 0xDC.toByte()
    var barg2: Byte = 0x65.toByte()
    var b1 = barg1 and barg2
    var b2 = barg1 or  barg2
    var b3 = barg1 xor barg2
    var b4 = barg1.inv()
    
    if (b1 != 0x44.toByte()) return "fail: Byte.and"
    if (b2 != 0xFD.toByte()) return "fail: Byte.or"
    if (b3 != 0xB9.toByte()) return "fail: Byte.xor"
    if (b4 != 0x23.toByte()) return "fail: Byte.inv"

    return "OK"
}