package test

class Outer<erased P>() : java.lang.Object() {
    class Inner() : java.lang.Object() {
        fun f<erased Q : P>() {}
    }
}
