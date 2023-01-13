// COMPILER_ARGUMENTS: -Xjvm-default=all-compatibility

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}

