// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

var cnt = 0

fun A?.getA(): A? {
    cnt++
    return this
}

fun get0(): Int {
    cnt++
    return 0
}

class A

var A?.b: A?
    get() {
        return this
    }
    set(v) {
    }

operator fun A?.get(i: Int): A? = this
operator fun A?.set(i: Int, v: A?): A? {
    return this
}

operator fun A?.plus(a: A?) = this

fun test(a: A?) {
    a?.getA().b += null
    a.getA().b += null

    a?.b[get0()] += null
    a.b[get0()] += null

    a?.b[get0()][get0()] += null
    a.b[get0()][get0()] += null
}

fun box(): String {
    test(null)
    if (cnt != 4) return "fail 1: $cnt"

    cnt = 0
    test(A())
    if (cnt != 8) return "fail 2: $cnt"

    return "OK"
}
