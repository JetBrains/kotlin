package test

open class Outer<erased P>() : java.lang.Object() {
    open class Inner<erased Q : P>() : java.lang.Object()
}
