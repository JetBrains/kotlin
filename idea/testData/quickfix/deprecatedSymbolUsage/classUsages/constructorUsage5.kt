// "Replace with 'A(s = "", i = { i }, i2 = 33)'" "true"

open class A(val s: String, val i: () -> Int, val i2: Int) {
    @Deprecated("Replace with primary constructor", ReplaceWith("A(s = \"\", i = { i }, i2 = 33)"))
    constructor(i: Int) : this("", { i }, i)
}

class B : A<caret>(i = 42)

fun a() {
    A(42)
}
