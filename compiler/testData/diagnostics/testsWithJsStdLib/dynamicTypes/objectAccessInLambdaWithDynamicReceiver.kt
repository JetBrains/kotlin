// FIR_IDENTICAL
// FIR_DUMP

fun jso(block: dynamic.() -> Unit): dynamic = js("({})").apply(block)

class G {
    companion object {
        val foo = "string"
    }
}

fun test() {
    jso {
        <!DEBUG_INFO_EXPRESSION_TYPE("G")!>G()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>G.foo<!>
    }
}