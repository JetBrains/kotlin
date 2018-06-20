// IGNORE_BACKEND: JS_IR
class IntRange {
    operator fun contains(a: Int) = (1..2).contains(a)
}

class C() {
    operator fun rangeTo(i: Int) = IntRange()
}


fun box(): String {
    if (2 in C()..2) {
        2 == 2
    }
    return "OK"
}
