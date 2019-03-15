// FILE: 1.kt
var result = ""

inline var apx:Int
    get() = 0
    set(value) { result = if (value == 1) "OK" else "fail" }


// FILE: 2.kt
fun test(s: Int?) {
    apx = s!!
}

fun box() : String {
    test(1)
    return result
}