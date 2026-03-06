// IGNORE_BACKEND_K1: ANY
// FIR_IDENTICAL

// IGNORE_BACKEND_KLIB: JS_IR

class A {
    @Suppress("INCONSISTENT_BACKING_FIELD_TYPE", "PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS", "VAR_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
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
