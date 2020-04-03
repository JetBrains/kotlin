// !DUMP_CFG
fun test_1(x: Int) {
    val y = when {
        x == 1 -> 10
        x % 2 == 0 -> 20
        1 - 1 == 0 -> return
        else -> 5
    }
}

interface A
interface B

fun test_2(x: Any?) {
    if (x is A && x is B) {
        x is A
    }
}