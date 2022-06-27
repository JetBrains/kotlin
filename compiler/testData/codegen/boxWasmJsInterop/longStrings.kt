// FILE: externals.js

function id(str) {
    return str
}

// FILE: externals.kt

external fun id(str: String): String

fun box(): String {
    var x = "1234567890"
    for (i in 1 until 20) {
        x += x
        val stringFromJs = id(x)
        if (!stringFromJs.equals(x)) return "FAIL"
    }
    return "OK"
}