// one.MyClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package one

@JvmInline
value class MyValueClass(val str: String)

class MyClass {
    companion object {
        @JvmStatic
        fun staticFunction(param: MyValueClass) {
        }

        @JvmStatic
        val staticProperty: MyValueClass? get() = null

        @JvmStatic
        val staticPropertyWithInitializer: MyValueClass? = null
    }
}
