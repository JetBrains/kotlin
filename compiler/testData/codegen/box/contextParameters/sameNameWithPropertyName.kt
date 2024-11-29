// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class A(var x: String){
    fun foo(): String { return x }
}

var result = ""

context(a: A)
var a: A
    get() = A("not OK")
    set(value) {
        result = a.foo()
    }

fun box(): String {
    with(A("OK")){
        a = A("not OK")
    }
    return result
}