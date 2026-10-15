// JVM_DEFAULT_MODE: no-compatibility

interface KtInterface {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}
