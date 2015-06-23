object A {
    var result = "Fail"
    
    fun foo() {
        result = "OK"
    }
}

fun box(): String {
    val x = A::foo
    x(A)
    return A.result
}
