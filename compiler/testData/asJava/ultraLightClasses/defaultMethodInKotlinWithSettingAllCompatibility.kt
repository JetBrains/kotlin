// CHECK_BY_JAVA_FILE
// COMPILER_ARGUMENTS: -Xjvm-default=all-compatibility

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}

