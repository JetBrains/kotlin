public open class Test() {
    open public fun test() : Unit {
        System.out?.println(hello)
    }
    default object {
        private val hello : String? = "Hello"
    }
}

fun box() : String {
    Test().test()
    return "OK"
}
