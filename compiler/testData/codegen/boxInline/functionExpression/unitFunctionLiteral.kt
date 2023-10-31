// FILE: 1.kt

inline fun <T : Unit?> myrun(block: () -> T) = block()

// FILE: 2.kt

fun box(): String {
    var x = "Fail"
    myrun(fun() {
        if (false) return
        x = "OK"
    })
    return x
}