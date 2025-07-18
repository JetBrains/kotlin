external abstract class A {
    // please check whether external abstract functions are OK for WASM (as they are prohibited for JVM)
    abstract fun foo()
}

fun box(): String {
    return "OK"
}