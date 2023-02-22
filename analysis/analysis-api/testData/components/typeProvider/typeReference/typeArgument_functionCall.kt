inline fun <T> foo(arg: T) {
    println(arg.toString())
}

fun test() {
    foo<Strin<caret>g>("42")
}