// FLOW: IN

interface I {
    var prop: Int
}

class C : I {
    override var prop: Int = 0
}

fun foo(i: I, c: C) {
    i.prop = 10
    val <caret>v = c.prop
}
