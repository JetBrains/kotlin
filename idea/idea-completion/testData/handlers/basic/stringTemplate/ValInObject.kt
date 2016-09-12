package p

object OOO {
    val vvv = 1
}

class C {
    fun foo() {
        "$v<caret>"
    }
}

// ELEMENT: vvv
// INVOCATION_COUNT: 2
