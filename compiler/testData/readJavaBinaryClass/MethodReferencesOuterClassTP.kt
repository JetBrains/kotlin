package test

class Outer<P>() : java.lang.Object() {
    class Inner() : java.lang.Object() {
        fun f<Q : P>() {}
    }
}
