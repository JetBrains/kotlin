object A {
    var result = "Fail"
    
    fun foo(newResult: String) {
        result = newResult
    }
}

fun box(): String {
    val x = A::foo
    A.x("OK")
    return A.result
}
