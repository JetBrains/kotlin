// "Replace usages of 'constructor A(Int)' in whole project" "true"

open class A(val s: String, val i: () -> Int, val i2: Int) {
    @Deprecated("Replace with primary constructor", ReplaceWith("C(s = \"\", a = { i }, m = i)"))
    constructor(i: Int) : this("", { i }, i)
}

open class C(val m: Int, val s: String, a: () -> Int)

class B : A<caret>(i = 31)

fun b() {
    val b = 30
    A(b)
}