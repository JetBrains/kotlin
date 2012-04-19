package test

trait Trait<erased P>

open class Outer<erased P, erased Q>() {
    open class Inner() {
    }
    
    open class Inner2() : Inner(), Trait<P> {
    }
}
