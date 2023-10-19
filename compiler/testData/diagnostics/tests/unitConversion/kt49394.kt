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
    handle(<!UNSUPPORTED_FEATURE("The feature \"unit conversions on arbitrary expressions\" is experimental and should be enabled explicitly. You can also change the original type of this expression to (...) -> Unit")!>x<!>)
}
