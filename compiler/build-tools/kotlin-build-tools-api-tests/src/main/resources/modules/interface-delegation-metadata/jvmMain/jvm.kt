actual interface BaseInterface {
    actual fun select(x: Any): String
    fun select(x: Int): String
}

actual object ImplementBase : BaseInterface {
    override fun select(x: Any): String = "common"
    override fun select(x: Int): String = "jvm"
}

fun main() {
    println("delegationResult=${test()}")
}
