// FILE: 1.kt

package test

fun calc() = "OK"

inline fun String.test() = this

class Test(
        p1: String = "1",
        p2: String = "2",
        p3: String = "3",
        p4: String = "4",
        p5: String = "5",
        p6: String = "6",
        p7: String = "7",
        p8: String = "8",
        p9: String = "9",
        p10: String = "10",
        p11: String = "11",
        p12: String = "12",
        p13: String = "13",
        val p14: String = "O".test(),
        p15: String = "15",
        p16: String = "16",
        p17: String = "17",
        p18: String = "18",
        p19: String = "19",
        p20: String = "20",
        p21: String = "21",
        p22: String = "22",
        p23: String = "23",
        p24: String = "24",
        p25: String = "25",
        p26: String = "26",
        p27: String = "27",
        p28: String = "28",
        p29: String = "29",
        p30: String = "30",
        p31: String = "31",
        p32: String = "32",
        val p33: String = "K".test()
)
// FILE: 2.kt

import test.*

fun box(): String {
    val test = Test()
    return test.p14 + test.p33
}