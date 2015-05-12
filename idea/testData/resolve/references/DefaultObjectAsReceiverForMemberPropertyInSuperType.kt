package t

interface Trait {
    val some : Int get() = 1
}

open class A {
    companion object Companion : Trait {

    }
}

fun test() {
    <caret>A.some
}


// REF: companion object of (t).A

