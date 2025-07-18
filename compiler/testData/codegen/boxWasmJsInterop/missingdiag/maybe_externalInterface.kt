// please check whether external interfaces are OK for WASM (as they are prohibited for JVM)
external interface A {
    val x: Int
        get
}

fun box(): String {
    return "OK"
}