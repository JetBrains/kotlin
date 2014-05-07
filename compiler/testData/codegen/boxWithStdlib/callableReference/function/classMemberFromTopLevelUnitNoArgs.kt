class A {
    var result = "Fail"
    
    fun foo() {
        result = "OK"
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x()
    return a.result
}
