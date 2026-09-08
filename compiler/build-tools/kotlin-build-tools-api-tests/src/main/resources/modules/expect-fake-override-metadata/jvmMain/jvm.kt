open class JvmDerived : Base() {
    fun select(x: B): String = "jvm"
}

actual class Derived actual constructor() : JvmDerived()

fun main() {
    println("fakeOverrideResult=${test()}")
}
