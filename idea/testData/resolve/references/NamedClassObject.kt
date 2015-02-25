package a

class A {
    class object Named {
        val i: Int
    }
}

fun main(args: Array<String>) {
    A.Na<caret>med.i
}

// REF: class object of (a).A