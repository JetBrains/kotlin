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

// DECLARATIONS_NO_LIGHT_ELEMENTS: MyClass.class[staticFunction;staticProperty]
// LIGHT_ELEMENTS_NO_DECLARATION: MyClass.class[getStaticProperty-BXGQg7w;getStaticProperty-BXGQg7w;getStaticPropertyWithInitializer-BXGQg7w;getStaticPropertyWithInitializer-BXGQg7w;staticFunction-rdfNfmQ;staticFunction-rdfNfmQ]
