// one.MyClass
// LANGUAGE: +ContextParameters +FullValueClasses
// WITH_STDLIB
package one

value class MyValueClass(val str: String, val count: Int)

class MyClass {
    context(a: MyValueClass)
    fun Boolean.contextAndReceiverAndValue(param: Long) {}

    context(a: MyValueClass)
    val Boolean.propertyContextReceiver: Int get() = 0
}
