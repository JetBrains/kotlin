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
    operator fun component1() = i + 1
    operator fun component2() = i + 2
    operator fun rangeTo(c: C) = Range(this, c)
}

fun doTest(): String {
    var s = ""
    for ((a, _) in C(0)..C(2)) {
        s += "$a;"
    }

    for ((_, b) in C(1)..C(3)) {
        s += "$b;"
    }

    for ((_, `_`) in C(2)..C(4)) {
        s += "$`_`;"
    }

    return s
}

fun box(): String {
    val s = doTest()
    return if (s == "1;2;3;3;4;5;4;5;6;") "OK" else "fail: $s"
}
