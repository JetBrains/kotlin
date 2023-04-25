// TARGET_FRONTEND: FIR
// FIR_IDENTICAL

// IGNORE_BACKEND_KLIB: JS_IR

class A {
    var it: Int
        field = 3.14
        get() = (field + 10).toInt()
        set(value) {
            field = (value - 10).toDouble()
        }
}

fun test() {
    val a = A()
    val it: Int = A().it and 10
    a.it = it
}
