// KT-42025

open class L<LL>(val ll: LL)

class Rec<T>(val rt: T)

public class Outer<OT>(val ot: OT) {
    fun <FT> bar(ft: FT): Outer<FT> {
        return foo1(ft, ::Outer)
    }

    fun <FT> local1(ft: FT): L<FT> {
        class Local1<LT>(val lt: LT, val ooot: OT): L<LT>(lt)
        return foo2(ft, ot, ::Local1)
    }

    fun <SS> createS(ss: SS): Static<SS> = foo1(ss, ::Static)

    fun <II> createI(ii: II) = foo2(ii, ot, ::Inner)

    public class Static<ST>(val st: ST) {
        public fun <FT> bar(fft: FT): Static<FT> {
            return foo1(fft, ::Static)
        }

        public fun <FT> local2(ft: FT): L<FT> {
            class Local2<LT>(val lt: LT, val sst: ST): L<LT>(lt)
            return foo2(ft, st, ::Local2)
        }
    }

    public inner class Inner<IT>(val it: IT, val oot: OT) {
        public fun <FT> bar(fft: FT): Inner<FT> {
            return foo2(fft, ot, ::Inner)
        }

        public fun <FT> local3(fft: FT): L<FT> {
            class Local3<LT>(val lt: LT, val iit: IT, val ooot: OT): L<LT>(lt)
            return foo3(fft, it, ot, ::Local3)
        }

        public fun <FT> local4(fft: FT): L<FT> {
            class Local4<LT>(val lt: LT, val iit: IT, val ooot: OT, val ffff: FT): L<LT>(lt)
            return foo4(fft, it, ot, fft, ::Local4)
        }

        public val <PT> Rec<PT>.p: L<PT>
            get() {
                class PLocal<LT>(lt: LT, val pt: PT): L<LT>(lt)
                return foo2(rt, rt, ::PLocal)
            }

        fun <PT> readP(r: Rec<PT>) = r.p
    }

}

fun <T1, R> foo1(t1: T1, bb: (T1) -> R): R = bb(t1)
fun <T1, T2, R> foo2(t1: T1, t2: T2, bb: (T1, T2) -> R): R = bb(t1, t2)
fun <T1, T2, T3, R> foo3(t1: T1, t2: T2, t3: T3, bb: (T1, T2, T3) -> R): R = bb(t1, t2, t3)
fun <T1, T2, T3, T4, R> foo4(t1: T1, t2: T2, t3: T3, t4: T4, bb: (T1, T2, T3, T4) -> R): R = bb(t1, t2, t3, t4)

fun box(): String {

    // outer
    val o = foo1(42, ::Outer)
    if (o.ot != 42) return "FAIL1: ${o.ot}"
    val ob = o.bar("42")
    if (ob.ot != "42") return "FAIL2: ${ob.ot}"
    val l1 = o.local1(42L)
    if (l1.ll != 42L) return "FAIL3: ${l1.ll}"

    // static
    val s = o.createS("ST")
    if (s.st != "ST") return "FAIL4: ${s.st}"
    val sb = s.bar("SB")
    if (sb.st != "SB") return "FAIL5: ${sb.st}"
    val sl = s.local2("SL")
    if (sl.ll != "SL") return "FAIL6: ${sl.ll}"

    // inner
    val i = o.createI("II")
    if (i.it != "II") return "FAIL7: ${i.it}"

    val ib = i.bar("IBar")
    if (ib.it != "IBar" && ib.oot != 42) return "FAIL8: ${ib.it} && ${ib.oot}"

    val il = i.local3("IL")
    if (il.ll != "IL") return "FAIL9: ${il.ll}"

    val il4 = i.local4("IL4")
    if (il4.ll != "IL4") return "FAIL10: ${il4.ll}"

    val ipl = i.readP(Rec("OK"))

    return ipl.ll
}
