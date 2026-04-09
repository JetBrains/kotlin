// ISSUE: KT-85601
// IGNORE_BACKEND: ANY
// WITH_STDLIB

var global = "FAIL"

fun getSizeWithSideEffect(): Int {
    global = "OK"
    return 42
}

fun box(): String {
    try {
        Array<String>(getSizeWithSideEffect(), TODO())
    } catch (e: NotImplementedError) {
    }
    return global
}
