package t

class A {
    companion object Companion {

    }

    class B
}

fun test() {
    <caret>A.B()
}


// REF: (t).A

