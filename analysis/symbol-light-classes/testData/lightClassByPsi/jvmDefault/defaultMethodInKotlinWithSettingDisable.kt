// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: disable

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun expressionBodyFun() = Unit

    fun withoutBody()

    val defaultProperty: Int
        get() = 1

    val propertyWithoutBody: Int

    companion object {
        @JvmStatic
        fun staticFun() {
        }

        @JvmStatic
        val staticProperty: Int = 1
    }
}
