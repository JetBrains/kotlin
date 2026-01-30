package java2d

class A {
    fun getConst() = OK

    companion object {
        const val OK = <!EVALUATED("OK")!>"OK"<!>
    }
}

fun box(): String {
    return A().getConst()
}
