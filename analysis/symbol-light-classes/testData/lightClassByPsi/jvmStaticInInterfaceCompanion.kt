// LIBRARY_PLATFORMS: JVM

interface KtInterface {
    fun defaultFun() {
    }

    companion object {
        @JvmStatic
        fun staticFun() {
        }

        @JvmStatic
        val staticProperty: Int = 1

        fun companionFun() {
        }
    }
}
