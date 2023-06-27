// KtInterface
// COMPILER_ARGUMENTS: -Xjvm-default=all-compatibility
// !JVM_DEFAULT_MODE: all-compatibility

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()

    val defaultProp: Int
        get() = 1

    val propWithoutBody: Int

    private fun privateFun() {}

    private val privateProp: Int
        get() = 1
}

