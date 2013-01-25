class A() {
    fun foo() {
        System.out?.println(1)
    }
}

fun box() : String {
    val a : A = A()
    return "OK"
}
