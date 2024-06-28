// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

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
