// FILE: 1.kt

package test

fun calc() = "OK"

inline fun test(
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
        p14: String = "14",
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
        p33: String = "33"
): String {
    return p1 + " " + p2 + " " + p3 + " " + p4 + " " + p5 + " " + p6 + " " +
           p7 + " " + p8 + " " + p9 + " " + p10 + " " + p11 + " " + p12 + " " + p13 + " " + p14 + " " +
           p15 + " " + p16 + " " + p17 + " " + p18 + " " + p19 + " " + p20 + " " + p21 + " " +
           p22 + " " + p23 + " " + p24 + " " + p25 + " " + p26 + " " + p27 + " " + p28 + " " +
           p29 + " " + p30 + " " + p31 + " " + p32 + " " + p33
}

// FILE: 2.kt

import test.*

fun box(): String {
    if (test() != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33")
        return "fail 1: ${test()}"

    if (test(p20 = "OK") != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 OK 21 22 23 24 25 26 27 28 29 30 31 32 33")
        return "fail 2: ${test(p20 = "OK")}"

    if (test(p20 = "O", p22 = "K") != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 O 21 K 23 24 25 26 27 28 29 30 31 32 33")
        return "fail 3: ${test(p20 = "O", p22 = "K")}"

    if (test(p20 = "O", p22 = "K", p32 = "23") != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 O 21 K 23 24 25 26 27 28 29 30 31 23 33")
        return "fail 4: ${test(p20 = "O", p22 = "K", p32 = "23")}"

    if (test(p20 = "O", p22 = "K", p32 = "33", p33 ="32") != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 O 21 K 23 24 25 26 27 28 29 30 31 33 32")
        return "fail 4: ${test(p20 = "O", p22 = "K", p32 = "33", p33 ="32")}"

    return "OK"
}