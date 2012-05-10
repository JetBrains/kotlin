package test

trait Trait<erased P> : java.lang.Object

open class Outer<erased P, erased Q>() : java.lang.Object() {
    open class Inner() : java.lang.Object() {
    }
    
    open class Inner2() : Inner(), Trait<P> {
    }
}
