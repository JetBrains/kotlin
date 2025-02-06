// LANGUAGE: +ContextReceivers
// ISSUE: KT-75016

interface I {
    context(String)
    var myProp: String
}

class C : I {
    var x = ""

    context(String)
    override var myProp: String
        get() = x
        set(value) {
            x = this@String + value
        }
}

fun box(): String {
    val c = C()
    return with("O") {
        c.myProp = "K"
        c.myProp
    }
}
