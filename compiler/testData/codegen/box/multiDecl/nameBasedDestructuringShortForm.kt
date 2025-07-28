// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +EnableNameBasedDestructuringShortForm

class Tuple(val first: String, val second: Int)

fun test1(x: Tuple): Boolean {
    val (first, second) = x
    return first == "OK" && second == 1
}

fun test2(x: Tuple): Boolean {
    val (second) = x
    return second == 1
}

fun test3(x: Tuple): Boolean {
    val (second: Int, first: String) = x
    return first == "OK" && second == 1
}

fun test4(x: Tuple): Boolean {
    var (first, second) = x
    first = "KO"
    second = 2

    return first == "KO" && second == 2
}

fun test5(x: Tuple): Boolean {
    for ((first, second) in arrayOf(x)) {
        if (first != "OK" || second != 1) return false
    }
    for ((second, first) in arrayOf(x)) {
        if (first != "OK" || second != 1) return false
    }
    for ((second: Int) in arrayOf(x)) {
        if (second != 1) return false
    }
    return true
}

fun test6(x: Tuple): Boolean {
    fun foo(f: (Tuple) -> Boolean) = f(x)

    return foo { (first, second) -> first == "OK" && second == 1 } &&
        foo { (second, first) -> first == "OK" && second == 1 } &&
        foo { (second) -> second == 1 }
}

fun box(): String {
    val x = Tuple("OK", 1)

    if (!test1(x)) return "FAIL 1"
    if (!test2(x)) return "FAIL 2"
    if (!test3(x)) return "FAIL 3"
    if (!test4(x)) return "FAIL 4"
    if (!test5(x)) return "FAIL 5"
    if (!test6(x)) return "FAIL 6"

    return "OK"
}