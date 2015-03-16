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
        useInt(<!DEBUG_INFO_SMARTCAST!>A.x<!>)
        useInt(<!TYPE_MISMATCH!>B.x<!>)
    }
}

fun useInt(i: Int) = i