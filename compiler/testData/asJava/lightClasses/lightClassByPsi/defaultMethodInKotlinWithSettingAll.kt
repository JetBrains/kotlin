// COMPILER_ARGUMENTS: -Xjvm-default=all
// !JVM_DEFAULT_MODE: all

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()

    private fun privateFun() {}
}

