actual class A {
    actual fun commonMember() { }

    fun platformMember() { }
}

fun test() {
    A().commonMember()
    A().platformMember()
}