// MODULE: original
fun <T> foo() where T : Int {}

// MODULE: copy
fun <T> foo() where T : Boolean {}