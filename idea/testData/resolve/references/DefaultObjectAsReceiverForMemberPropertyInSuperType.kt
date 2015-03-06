package t

trait Trait {
    val some : Int get() = 1
}

open class A {
    class object Default : Trait {

    }
}

fun test() {
    <caret>A.some
}


// REF: class object of (t).A

