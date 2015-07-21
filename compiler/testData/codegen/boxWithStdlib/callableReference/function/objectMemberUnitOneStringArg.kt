object A {
    var result = "Fail"
    
    fun foo(newResult: String) {
        result = newResult
    }
}

fun box(): String {
    val x = A::foo
    x(A, "OK")
    return A.result
}
