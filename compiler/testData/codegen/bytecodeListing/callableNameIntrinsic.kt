class A {
    val a = ""
    fun b() = ""

    fun test() {
        val a = A::a.name
        val b = A::b.name
        val c = ::A.name
    }
}
