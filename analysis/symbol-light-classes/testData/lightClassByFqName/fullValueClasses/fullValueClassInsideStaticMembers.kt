// one.MyClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package one

value class MyValueClass(val str: String, val count: Int)

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
