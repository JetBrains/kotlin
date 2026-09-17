// one.MyClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package one

value class MyValueClass(val str: String, val count: Int)

interface BaseInterface {
    fun regularFunction()
    fun functionWithValueClassParameter(param: MyValueClass)
    val propertyWithValueClassParameter: MyValueClass?
}

class MyClass(b: BaseInterface) : BaseInterface by b
