//WITH_STDLIB

class SomeClass(val value: Int)

fun f1(vararg x: Int): Int = x.size

fun f2(vararg x: String): Int = x.size

fun f3(vararg x: UInt): Int = x.size

fun f4(vararg x: SomeClass): Int = x.size

fun f5(a: Int, vararg x: Int): Int = x.size + a

fun box(): String {
    if (f1() != 0) return "1"
    if (f2() != 0) return "2"
    if (f3() != 0) return "3"
    if (f4() != 0) return "4"
    if (f5(0) != 0) return "5"
    if (f5(f1()) != 0) return "7"
    if (f5(f2()) != 0) return "7"
    if (f5(f3()) != 0) return "8"
    if (f5(f4()) != 0) return "9"
    if (f5(f5(0)) != 0) return "10"
    if (f5(f1(1,2,3)) != 3) return "11"
    if (f5(f2("1", "2", "3")) != 3) return "12"
    if (f5(f3(1U, 2U, 3U)) != 3) return "13"
    if (f5(f4(SomeClass(1),SomeClass(2),SomeClass(3))) != 3) return "15"
    if (f5(f5(1, 2, 3)) != 3) return "16"

    return "OK"
}