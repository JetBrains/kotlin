// IGNORE_BACKEND_FIR: JVM_IR
class A {
    var result = "Fail"
    
    fun foo(newResult: String) {
        result = newResult
    }
}

fun box(): String {
    val a = A()
    val x = A::foo
    x(a, "OK")
    return a.result
}
