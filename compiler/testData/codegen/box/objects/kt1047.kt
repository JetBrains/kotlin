// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

public open class Test() {
    open public fun test() : Unit {
        System.out?.println(hello)
    }
    companion object {
        private val hello : String? = "Hello"
    }
}

fun box() : String {
    Test().test()
    return "OK"
}
