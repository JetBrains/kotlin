// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun test1(x: Tuple): Boolean {
    [val a, val b] = x
    return a == "OK" && b == 1
}

fun test2(x: Tuple): Boolean {
    [val b] = x
    return b == "OK"
}

fun test3(x: Tuple): Boolean {
    [val b: String, val a: Int] = x
    return b == "OK" && a == 1
}

fun test4(x: Tuple): Boolean {
    [var a, var b] = x
    a = "KO"
    b = 2

    return a == "KO" && b == 2
}

fun test5(x: Tuple): Boolean {
    for ([val a, val b] in arrayOf(x)) {
        if (a != "OK" || b != 1) return false
    }
    for ([val b, val a] in arrayOf(x)) {
        if (b != "OK" || a != 1) return false
    }
    for ([val b: String] in arrayOf(x)) {
        if (b != "OK") return false
    }
    return true
}

fun test6(x: Tuple): Boolean {
    fun foo(f: (Tuple) -> Boolean) = f(x)

    return foo { [val a, val b] -> a == "OK" && b == 1 } &&
        foo { [val b, val a] -> b == "OK" && a == 1 } &&
        foo { [val b] -> b == "OK" }
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