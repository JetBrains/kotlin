package t

trait Trait

open class A {
    class object Default : Trait {

    }
}

fun Trait.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: class object of (t).A

