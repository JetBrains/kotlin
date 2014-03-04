// "Convert to block body" "true"
var foo: String
    get() = "abc"
    set(value)  {
        doSet(value)
    }

fun doSet(value: String){}