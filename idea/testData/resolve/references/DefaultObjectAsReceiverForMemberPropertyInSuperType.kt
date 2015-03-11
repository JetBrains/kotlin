package t

trait Trait {
    val some : Int get() = 1
}

open class A {
    default object Default : Trait {

    }
}

fun test() {
    <caret>A.some
}


// REF: default object of (t).A

