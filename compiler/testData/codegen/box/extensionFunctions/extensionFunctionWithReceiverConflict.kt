class A {
    class A
    fun A.foo(): String {
        return "OK"
    }
}

fun box(): String {
    with(A()){
        return A.A().foo()
    }
}