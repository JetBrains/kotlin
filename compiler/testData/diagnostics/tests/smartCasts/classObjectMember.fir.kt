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
        useInt(B.x)
    }
}

fun useInt(i: Int) = i