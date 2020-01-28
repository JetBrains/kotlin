open class T {
    val x : Int? = null
}

class A {
    companion object: T() {
    }
}

class B {
    companion object: T() {
    }
}

fun test() {
    if (A.x != null) {
        useInt(A.x)
        <!INAPPLICABLE_CANDIDATE!>useInt<!>(B.x)
    }
}

fun useInt(i: Int) = i