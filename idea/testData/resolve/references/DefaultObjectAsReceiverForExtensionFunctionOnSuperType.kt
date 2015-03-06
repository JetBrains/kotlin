package t

trait Trait

open class A {
    default object Default : Trait {

    }
}

fun Trait.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: default object of (t).A

