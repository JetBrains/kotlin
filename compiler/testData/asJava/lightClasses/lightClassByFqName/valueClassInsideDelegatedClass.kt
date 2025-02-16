// one.MyClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package one

@JvmInline
value class MyValueClass(val str: String)

interface BaseInterface {
    fun regularFunction()
    fun functionWithValueClassParameter(param: MyValueClass)
    val propertyWithValueClassParameter: MyValueClass?
}

class MyClass(b: BaseInterface) : BaseInterface by b