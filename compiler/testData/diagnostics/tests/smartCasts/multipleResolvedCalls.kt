interface I1
interface I2
interface I3
interface I4
interface I5

operator fun I1.component1() = 1
operator fun I2.component2() = 2
operator fun I3.component3() = 3
operator fun I4.component4() = 4
operator fun I5.component5() = 5

fun test(x: Any): Int {
    if (x is I1 && x is I2 && x is I3 && x is I4 && x is I5) {
        val (t1, t2, t3, t4, t5) = <!DEBUG_INFO_SMARTCAST!>x<!>
        return t1 + t2 + t3 + t4 + t5
    }
    else return 0
}
