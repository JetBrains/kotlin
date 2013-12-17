object A {
    var result = "Fail"
    
    fun foo() {
        result = "OK"
    }
}

fun box(): String {
    val x = A::foo
    A.x()
    return A.result
}
