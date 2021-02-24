enum class Test(val x: String, val closure1: () -> String) {
    FOO("O", { FOO.x }) {
        override val y: String = "K"
        val closure2 = { y } // Implicit 'FOO'
        override val z: String = closure2()
    };

    abstract val y: String
    abstract val z: String

    fun test() = closure1() + z
}

fun box() = Test.FOO.test()