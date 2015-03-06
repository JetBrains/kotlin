package a

class A {
    default object Named {
        val i: Int
    }
}

fun main(args: Array<String>) {
    A.Na<caret>med.i
}

// REF: default object of (a).A