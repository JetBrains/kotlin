actual class Foo actual constructor() {
    actual constructor(s: String) : this()

    actual fun nonPlatformFun() {}

    actual val nonPlatformVal = ""

    private fun nonImplFun() {}
    private val nonImplVal = ""
}
