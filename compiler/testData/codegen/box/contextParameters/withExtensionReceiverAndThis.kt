// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-73779
// LANGUAGE: +ContextParameters

class C(val a: String) {
    fun foo(): String { return a }
}

context(c: C)
fun C.function(): String {
    return this.foo()
}

context(c: C)
val C.property: String
    get() = this.foo()

fun box(): String {
    with(C("not OK")) {
        if ((C("OK").function() == "OK") &&
            (C("OK").property == "OK")
        ) return "OK"
    }
    return "NOK"
}