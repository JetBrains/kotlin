package test

open class Outer<erased P>() {
    open class Inner<erased Q : P>()
}
