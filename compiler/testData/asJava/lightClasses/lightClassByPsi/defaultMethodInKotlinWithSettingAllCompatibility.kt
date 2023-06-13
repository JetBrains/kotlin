// COMPILER_ARGUMENTS: -Xjvm-default=all-compatibility
// !JVM_DEFAULT_MODE: all-compatibility

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()

    private fun privateFun() {}
}

