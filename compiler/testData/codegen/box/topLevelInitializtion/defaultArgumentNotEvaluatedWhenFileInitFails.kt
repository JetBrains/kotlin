// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// KT-89489, KT-89487: Fixed in 2.5.20-Beta1
// FILE: another.kt

private val prop: String = computeProp()

fun foo(unused: Unit = boo()) {
    log("foo")
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

fun computeProp(): String {
    log("prop")
    throw IllegalStateException("1")
}

fun boo() {
    log("boo")
}

// FILE: main.kt

fun box(): String {
    log("S")
    try {
        foo()
        return "Fail: foo() must throw"
    } catch (t: Throwable) {
        log("caught")
    }
    log("E")

    if (l != "S;prop;caught;E;") return l
    return "OK"
}
