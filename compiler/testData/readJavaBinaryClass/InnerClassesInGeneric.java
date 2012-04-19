package test;

interface Trait<P> {
}

class Outer<P, Q> {
    class Inner {
    }
    
    class Inner2 extends Inner implements Trait<P> {
    }
}
