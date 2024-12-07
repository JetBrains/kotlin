package a

class A {
    companion object Named {
        val i: Int = 1
    }
}

fun main(args: Array<String>) {
    A.Na<caret>med.i
}

