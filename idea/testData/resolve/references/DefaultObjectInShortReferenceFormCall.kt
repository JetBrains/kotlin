package t

class A {
    class object Default {
        fun test()
    }
}

fun test() {
    <caret>A.test()
}


// REF: class object of (t).A

