// FILE: 1.kt

public inline fun Int.times2(body : () -> Unit) {
    var count = this;
    while (count > 0) {
        body()
        count--
    }
}

// FILE: 2.kt

fun test1(): Int {
    var s = 0;
    2.times2 {
        s++
    }
    return s;
}

fun box(): String {
    if (test1() != 2) return "test1: ${test1()}"

    return "OK"
}
