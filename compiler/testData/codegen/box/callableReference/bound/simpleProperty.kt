class A(val res: Int) {
    fun foo() = res
}


fun box(): String {
//    val a = A()
    var a = "kotlin".length
    print(a)
    val g = A::foo
    print(g(A(78)))

    val f = (if (1 < 2) A(6) else { print(1); A(2)})::foo
    val result = f()
    return if (result == 6) "OK" else "Fail: $result"
}
