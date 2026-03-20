// LANGUAGE: +ValueClasses
// WITH_STDLIB

val globalIntsLog = mutableListOf<Int>()

fun runAndLog(block: () -> Unit): List<Int> {
    globalIntsLog.clear()
    block()
    return globalIntsLog.toList()
}

abstract value class AbstractValue(p0: Int, p1: Int) {
    init {
        globalIntsLog += p0
        globalIntsLog += p1
    }
    abstract val p2: Int
    open val p4: Int get() = -1
    val p6: Int get() = -2
}

sealed value class SealedValue(s0: Int, s1: Int, s2: Int, s3: Int) : AbstractValue(s0, s1) {
    init {
        globalIntsLog += s0
        globalIntsLog += s1
        globalIntsLog += s2
        globalIntsLog += s3
    }
    abstract val s4: Int
    open val s6: Int get() = -3
    val s8: Int get() = -4
}

sealed class SealedIdentity(val s0: Int, val s1: Int, val s2: Int, val s3: Int) : AbstractValue(s0, s1) {
    abstract val s4: Int
    open val s5: Int = -5
    open val s6: Int get() = -6
    val s7: Int = -7
    val s8: Int get() = -8
    val s9: Int = -9
    val s10: Int = -10
    val s11 by lazy { -11 }
    var s12: Int = -12
        get() = field
        set(value) { field = value }
}

value class FinalValue(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override val s4 get() = -13
    val t9: Int get() = -14
    override val p2: Int
        get() = TODO("Not yet implemented")
}

open class OpenIdentity(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override val p2 = -15
    override val s4 get() = -16
    open val t6: Int = -17
    open val t7: Int get() = -18
    val t8: Int = -19
    val t9: Int get() = -20
    val t10: Int = -21
    val t11: Int = -22
    val t12 by lazy { -23 }
    var t13: Int = -24
        get() = field
        set(value) { field = value }
}

class FinalIdentity(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override val p2 = -25
    override val s4 get() = -26
    open val t6: Int = -27
    open val t7: Int get() = -28
    val t8: Int = -29
    val t9: Int get() = -30
    val t10: Int = -31
    val t11: Int = -32
    val t12 by lazy { -33 }
    var t13: Int = -34
        get() = field
        set(value) { field = value }
}

class FromSealedIdentity(s0: Int, s1: Int, s2: Int, s3: Int): SealedIdentity(s0, s1, s2, s3) {
    override val p2: Int
        get() = -35
    override val s4: Int
        get() = -36
}

class SimpleClass(p0: Int, val p1: Int) : AbstractValue(p0, p1) {
    override val p2: Int
        get() = p1
}

fun box(): String {
    // FinalValue: value class inheriting SealedValue -> AbstractValue
    val fvInts = runAndLog { FinalValue(1, 2, 3, 4, 5, 6) }
    val fv = FinalValue(1, 2, 3, 4, 5, 6)
    if (fvInts != listOf(1, 2, 1, 2, 3, 4)) return "Fail: FinalValue log = $fvInts"
    if (fv.t0 != 1) return "Fail: FinalValue.t0 = ${fv.t0}"
    if (fv.t1 != 2) return "Fail: FinalValue.t1 = ${fv.t1}"
    if (fv.t2 != 3) return "Fail: FinalValue.t2 = ${fv.t2}"
    if (fv.t3 != 4) return "Fail: FinalValue.t3 = ${fv.t3}"
    if (fv.t4 != 5) return "Fail: FinalValue.t4 = ${fv.t4}"
    if (fv.t5 != 6) return "Fail: FinalValue.t5 = ${fv.t5}"
    // Inherited from SealedValue (constructor params are not properties anymore)
    if (fv.s4 != -13) return "Fail: FinalValue.s4 = ${fv.s4}"
    if (fv.s6 != -3) return "Fail: FinalValue.s6 = ${fv.s6}"
    if (fv.s8 != -4) return "Fail: FinalValue.s8 = ${fv.s8}"
    if (fv.t9 != -14) return "Fail: FinalValue.t9 = ${fv.t9}"
    // Inherited from AbstractValue (constructor params are not properties anymore)
    if (fv.p4 != -1) return "Fail: FinalValue.p4 = ${fv.p4}"
    if (fv.p6 != -2) return "Fail: FinalValue.p6 = ${fv.p6}"
    if (!fv.equals(fv)) return "Fail: FinalValue.equals"
    if (fv.hashCode() != fv.hashCode()) return "Fail: FinalValue.hashCode"
    if (fv.toString() != fv.toString()) return "Fail: FinalValue.toString"


    // OpenIdentity: open class inheriting SealedValue -> AbstractValue
    val oiInts = runAndLog { OpenIdentity(1, 2, 3, 4, 5, 6) }
    if (oiInts != listOf(1, 2, 1, 2, 3, 4)) return "Fail: OpenIdentity log = $oiInts"
    val oi = OpenIdentity(1, 2, 3, 4, 5, 6)
    if (oi.t0 != 1) return "Fail: OpenIdentity.t0 = ${oi.t0}"
    if (oi.t1 != 2) return "Fail: OpenIdentity.t1 = ${oi.t1}"
    if (oi.t2 != 3) return "Fail: OpenIdentity.t2 = ${oi.t2}"
    if (oi.t3 != 4) return "Fail: OpenIdentity.t3 = ${oi.t3}"
    if (oi.t4 != 5) return "Fail: OpenIdentity.t4 = ${oi.t4}"
    if (oi.t5 != 6) return "Fail: OpenIdentity.t5 = ${oi.t5}"
    if (oi.p2 != -15) return "Fail: OpenIdentity.p2 = ${oi.p2}"
    if (oi.s4 != -16) return "Fail: OpenIdentity.s4 = ${oi.s4}"
    if (oi.t6 != -17) return "Fail: OpenIdentity.t6 = ${oi.t6}"
    if (oi.t7 != -18) return "Fail: OpenIdentity.t7 = ${oi.t7}"
    if (oi.t8 != -19) return "Fail: OpenIdentity.t8 = ${oi.t8}"
    if (oi.t9 != -20) return "Fail: OpenIdentity.t9 = ${oi.t9}"
    if (oi.t10 != -21) return "Fail: OpenIdentity.t10 = ${oi.t10}"
    if (oi.t11 != -22) return "Fail: OpenIdentity.t11 = ${oi.t11}"
    if (oi.t12 != -23) return "Fail: OpenIdentity.t12 = ${oi.t12}"
    // Read var t13
    if (oi.t13 != -24) return "Fail: OpenIdentity.t13 read = ${oi.t13}"
    // Write var t13
    oi.t13 = 42
    if (oi.t13 != 42) return "Fail: OpenIdentity.t13 write = ${oi.t13}"
    // Inherited from SealedValue (constructor params are not properties anymore)
    if (oi.s6 != -3) return "Fail: OpenIdentity.s6 = ${oi.s6}"
    if (oi.s8 != -4) return "Fail: OpenIdentity.s8 = ${oi.s8}"
    // Inherited from AbstractValue
    if (oi.p4 != -1) return "Fail: OpenIdentity.p4 = ${oi.p4}"
    if (oi.p6 != -2) return "Fail: OpenIdentity.p6 = ${oi.p6}"
    if (!oi.equals(oi)) return "Fail: OpenIdentity.equals"
    if (oi.hashCode() != oi.hashCode()) return "Fail: OpenIdentity.hashCode"
    if (oi.toString() != oi.toString()) return "Fail: OpenIdentity.toString"

    // FinalIdentity: final class inheriting SealedValue -> AbstractValue
    val fiInts = runAndLog { FinalIdentity(1, 2, 3, 4, 5, 6) }
    if (fiInts != listOf(1, 2, 1, 2, 3, 4)) return "Fail: FinalIdentity log = $fiInts"
    val fi = FinalIdentity(1, 2, 3, 4, 5, 6)
    if (fi.t0 != 1) return "Fail: FinalIdentity.t0 = ${fi.t0}"
    if (fi.t1 != 2) return "Fail: FinalIdentity.t1 = ${fi.t1}"
    if (fi.t2 != 3) return "Fail: FinalIdentity.t2 = ${fi.t2}"
    if (fi.t3 != 4) return "Fail: FinalIdentity.t3 = ${fi.t3}"
    if (fi.t4 != 5) return "Fail: FinalIdentity.t4 = ${fi.t4}"
    if (fi.t5 != 6) return "Fail: FinalIdentity.t5 = ${fi.t5}"
    if (fi.p2 != -25) return "Fail: FinalIdentity.p2 = ${fi.p2}"
    if (fi.s4 != -26) return "Fail: FinalIdentity.s4 = ${fi.s4}"
    if (fi.t6 != -27) return "Fail: FinalIdentity.t6 = ${fi.t6}"
    if (fi.t7 != -28) return "Fail: FinalIdentity.t7 = ${fi.t7}"
    if (fi.t8 != -29) return "Fail: FinalIdentity.t8 = ${fi.t8}"
    if (fi.t9 != -30) return "Fail: FinalIdentity.t9 = ${fi.t9}"
    if (fi.t10 != -31) return "Fail: FinalIdentity.t10 = ${fi.t10}"
    if (fi.t11 != -32) return "Fail: FinalIdentity.t11 = ${fi.t11}"
    if (fi.t12 != -33) return "Fail: FinalIdentity.t12 = ${fi.t12}"
    // Read var t13
    if (fi.t13 != -34) return "Fail: FinalIdentity.t13 read = ${fi.t13}"
    // Write var t13
    fi.t13 = 99
    if (fi.t13 != 99) return "Fail: FinalIdentity.t13 write = ${fi.t13}"
    // Inherited from SealedValue (constructor params are not properties anymore)
    if (fi.s6 != -3) return "Fail: FinalIdentity.s6 = ${fi.s6}"
    if (fi.s8 != -4) return "Fail: FinalIdentity.s8 = ${fi.s8}"
    // Inherited from AbstractValue
    if (fi.p4 != -1) return "Fail: FinalIdentity.p4 = ${fi.p4}"
    if (fi.p6 != -2) return "Fail: FinalIdentity.p6 = ${fi.p6}"
    if (!fi.equals(fi)) return "Fail: FinalIdentity.equals"
    if (fi.hashCode() != fi.hashCode()) return "Fail: FinalIdentity.hashCode"
    if (fi.toString() != fi.toString()) return "Fail: FinalIdentity.toString"


    val fsiInts = runAndLog { FromSealedIdentity(1, 2, 3, 4) }
    if (fsiInts != listOf(1, 2)) return "Fail: FromSealedIdentity log = $fsiInts"
    val fromSealedIdentity = FromSealedIdentity(1, 2, 3, 4)
    if (fromSealedIdentity.s5 != -5) return "Fail: FromSealedIdentity.s5 = ${fromSealedIdentity.s5}"
    if (fromSealedIdentity.s6 != -6) return "Fail: FromSealedIdentity.s6 = ${fromSealedIdentity.s6}"
    if (fromSealedIdentity.s7 != -7) return "Fail: FromSealedIdentity.s7 = ${fromSealedIdentity.s7}"
    if (fromSealedIdentity.s8 != -8) return "Fail: FromSealedIdentity.s8 = ${fromSealedIdentity.s8}"
    if (fromSealedIdentity.s9 != -9) return "Fail: FromSealedIdentity.s9 = ${fromSealedIdentity.s9}"
    if (fromSealedIdentity.s10 != -10) return "Fail: FromSealedIdentity.s10 = ${fromSealedIdentity.s10}"
    if (fromSealedIdentity.s11 != -11) return "Fail: FromSealedIdentity.s11 = ${fromSealedIdentity.s11}"
    // Read var s12
    if (fromSealedIdentity.s12 != -12) return "Fail: FromSealedIdentity.s12 read = ${fromSealedIdentity.s12}"
    // Write var s12
    fromSealedIdentity.s12 = 50
    if (fromSealedIdentity.s12 != 50) return "Fail: FromSealedIdentity.s12 write = ${fromSealedIdentity.s12}"
    // Inherited from AbstractValue (constructor params are not properties anymore)
    if (fromSealedIdentity.p4 != -1) return "Fail: FromSealedIdentity.p4"
    if (fromSealedIdentity.p6 != -2) return "Fail: FromSealedIdentity.p6"
    if (fromSealedIdentity.p2 != -35) return "Fail: FromSealedIdentity.p2"
    if (fromSealedIdentity.s4 != -36) return "Fail: FromSealedIdentity.s4"
    if (!fromSealedIdentity.equals(fromSealedIdentity)) return "Fail: FromSealedIdentity.equals"
    if (fromSealedIdentity.hashCode() != fromSealedIdentity.hashCode()) return "Fail: FromSealedIdentity.hashCode"
    if (fromSealedIdentity.toString() != fromSealedIdentity.toString()) return "Fail: FromSealedIdentity.toString"

    val sc1 = SimpleClass(1, 2)
    val sc2 = SimpleClass(1, 2)
    if (sc1 == sc2) return "Fail: equals should be false, got true"
    if (sc1.hashCode() == sc2.hashCode()) return "Fail: hash should differ, got ${sc1.hashCode()} == ${sc2.hashCode()}"
    if (sc1.toString() == sc2.toString() && sc1.toString() != "[object Object]") return "Fail: toString should differ, got '${sc1.toString()}' == '${sc2.toString()}'"
    if (sc1.p2 != 2) return "Fail: p2 = ${sc1.p2}"

    return "OK"
}
