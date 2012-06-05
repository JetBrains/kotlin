package test

open class Outer<P>() : java.lang.Object() {
    open class Inner<Q : P>() : java.lang.Object()
}
