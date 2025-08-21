package java2d

class A {
    fun getConst() = <!EVALUATED{IR}("OK")!>OK<!>

    companion object {
        const val OK = <!EVALUATED("OK")!>"OK"<!>
    }
}

fun box(): String {
    return A().getConst()
}
