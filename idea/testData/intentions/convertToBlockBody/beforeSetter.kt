// "Convert to block body" "true"
var foo: String
    get() = "abc"
    <caret>set(value) = doSet(value)

fun doSet(value: String){}