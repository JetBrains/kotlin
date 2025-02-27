// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-75016

interface I {
    context(s: String)
    var myProp: String
}

class C : I {
    var x = ""

    context(s: String)
    override var myProp: String
        get() = x
        set(value) {
            x = s + value
        }
}

fun box(): String {
    val c = C()
    return with("O") {
        c.myProp = "K"
        c.myProp
    }
}