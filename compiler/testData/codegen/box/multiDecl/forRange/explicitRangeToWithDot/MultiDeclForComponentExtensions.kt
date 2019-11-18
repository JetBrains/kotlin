// IGNORE_BACKEND_FIR: JVM_IR
class Range(val from : C, val to: C) {
    operator fun iterator() = It(from, to)
}

class It(val from: C, val to: C) {
    var c = from.i

    operator fun next(): C {
        val next = C(c)
        c++
        return next
    }

    operator fun hasNext(): Boolean = c <= to.i
}

class C(val i : Int) {
    fun rangeTo(c: C) = Range(this, c)
}
operator fun C.component1() = i + 1
operator fun C.component2() = i + 2

fun doTest(): String {
    var s = ""
    for ((a, b) in C(0).rangeTo(C(2))) {
        s += "$a:$b;"
    }
    return s
}

fun box(): String {
    val s = doTest()
    return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}
