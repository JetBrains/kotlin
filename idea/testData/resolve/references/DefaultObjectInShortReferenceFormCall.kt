package t

class A {
    default object Default {
        fun test()
    }
}

fun test() {
    <caret>A.test()
}


// REF: default object of (t).A

