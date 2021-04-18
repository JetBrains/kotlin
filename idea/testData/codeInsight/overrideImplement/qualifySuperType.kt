// FIR_IDENTICAL
class Outer {
    interface Inner1 {
        fun f() { }
    }

    interface Inner2 {
        fun g() { }
    }
}

class X : Outer.Inner1, Outer.Inner2 {
    <caret>
}