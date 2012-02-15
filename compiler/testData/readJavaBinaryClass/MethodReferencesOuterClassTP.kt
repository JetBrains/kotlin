package test

class Outer<erased P>() {
    class Inner() {
        fun f<erased Q : P>() {}
    }
}
