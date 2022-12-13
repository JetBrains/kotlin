// IGNORE_BACKEND: WASM
// KT-55468
// IGNORE_BACKEND_K2: NATIVE

fun <T> myRun(action: () -> T): T = action()
fun foo(): String = "foo"

fun <K> materialize(): K {
    result += "K"
    return "str" as K
}

var result = "fail"

fun test1(n: Number, b: Boolean) {
    n.let {
        if (b) return@let

        myRun {
            result = "O"
            foo()
        }
    }
}

fun test2(n: Number, b: Boolean) {
    n.let {
        if (b) return@let
        materialize()
    }
}

fun box(): String {
    test1(42, false)
    test2(42, false)
    return result
}