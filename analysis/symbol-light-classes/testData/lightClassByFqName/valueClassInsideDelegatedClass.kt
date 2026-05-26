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
// DECLARATIONS_NO_LIGHT_ELEMENTS: MyClass.class[functionWithValueClassParameter;propertyWithValueClassParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: MyClass.class[functionWithValueClassParameter-rdfNfmQ;getPropertyWithValueClassParameter-BXGQg7w]