// "Replace usages of 'constructor A(Int)' in whole project" "true"

open class A(val b: String, val i: () -> Int) {
    @Deprecated("Replace with primary constructor", ReplaceWith("A(b = \"\") { i }"))
    constructor(i: Int) : this("", { i })
}

class B : A<caret>(i = 33)

fun a() {
    A(42)
}
