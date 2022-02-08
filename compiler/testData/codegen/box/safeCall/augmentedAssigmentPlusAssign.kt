// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR

var cnt = 0

class A

var A?.b: A?
    get() {
        return this
    }
    set(v) {
        cnt++
    }

var A?.c: A?
    get() {
        return this
    }
    set(v) {
        cnt++
    }

operator fun A?.get(i: Int): A? = this

operator fun A?.plusAssign(a: A?) {
    cnt++
}

fun test(a: A?) {
    a?.b += null
    a?.b?.c += null
    a?.b.c += null // ".c" will be called anyway

    a?.b[0] += null
    a?.b?.c[0] += null
    a?.b.c[0] += null // ".c" will be called anyway

    a?.b[0][0] += null
    a?.b?.c[0][0] += null
    a?.b.c[0][0] += null // ".c" will be called anyway
}

fun box(): String {
    test(null)
    if (cnt != 3) return "fail 1: $cnt"

    cnt = 0
    test(A())
    if (cnt != 9) return "fail 2: $cnt"

    return "OK"
}
