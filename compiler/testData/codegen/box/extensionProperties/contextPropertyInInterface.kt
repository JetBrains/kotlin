// LANGUAGE: +ContextReceivers
// ISSUE: KT-75016
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ KT-75016: Compiler v2.1.10 has a bug in 1st compilation phase

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
