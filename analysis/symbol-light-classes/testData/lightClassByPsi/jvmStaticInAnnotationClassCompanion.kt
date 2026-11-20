// LIBRARY_PLATFORMS: JVM

import kotlin.reflect.KClass

annotation class KtAnnotation(val i: Int) {
    companion object {
        @JvmStatic
        fun staticFun(s: String, k: KClass<*>): KClass<*> = k

        @JvmStatic
        val staticProperty: Int = 1

        @JvmStatic
        val stringProperty: String = ""

        @JvmStatic
        val kClassProperty: KClass<*> = Int::class

        @JvmStatic
        var staticVariable: String
            get() = ""
            set(value) {}

        @JvmStatic
        lateinit var lateinitVariable: String

        const val CONSTANT: Int = 2

        fun companionFun() {
        }
    }
}
