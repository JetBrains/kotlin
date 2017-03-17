class Outer {
    open inner class Inner
    inner class InnerDerived0 : Inner()
    inner class InnerDerived1 : OI()
}

typealias OI = Outer.Inner

fun test() = Outer().OI()