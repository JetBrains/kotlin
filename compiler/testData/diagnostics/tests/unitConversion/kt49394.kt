// RUN_PIPELINE_TILL: FRONTEND
fun interface Run {
    fun run()
}

fun handle(run: Run) {
    //...
}

val x = {
    "STRING"
}

fun test() {
    handle(<!UNSUPPORTED_FEATURE("The feature \"unit conversions on arbitrary expressions\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+UnitConversionsOnArbitraryExpressions', but note that no stability guarantees are provided.. You can also change the original type of this expression to (...) -> Unit")!>x<!>)
}
