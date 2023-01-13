// COMPILER_ARGUMENTS: -Xjvm-default=all

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}

