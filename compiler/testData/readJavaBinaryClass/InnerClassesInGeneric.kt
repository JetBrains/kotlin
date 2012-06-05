package test

trait Trait<P> : java.lang.Object

open class Outer<P, Q>() : java.lang.Object() {
    open class Inner() : java.lang.Object() {
    }
    
    open class Inner2() : Inner(), Trait<P> {
    }
}
