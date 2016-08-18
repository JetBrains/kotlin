interface IA

interface IB {
    operator fun IA.compareTo(other: IA): Int
}

fun IB.test1(a1: IA, a2: IA) = a1 > a2
fun IB.test2(a1: IA, a2: IA) = a1 >= a2
fun IB.test3(a1: IA, a2: IA) = a1 < a2
fun IB.test4(a1: IA, a2: IA) = a1 <= a2