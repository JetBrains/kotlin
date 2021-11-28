// FIR_IDENTICAL
// WITH_STDLIB

interface C {
    val test1: Int
    val test2: Int get() = 0
    var test3: Int
    var test4: Int get() = 0; set(value) {}
}