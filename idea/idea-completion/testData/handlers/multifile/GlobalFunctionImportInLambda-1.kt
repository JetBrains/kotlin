// FIR_COMPARISON
object XXX {
    fun authorize(handler: String.() -> Unit) { }
}

fun f() {
    XXX.authorize {
        globalFun<caret>
    }
}
