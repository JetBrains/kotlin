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
    handle(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}