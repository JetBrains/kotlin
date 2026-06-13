// one.MyClass
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class MyValueClass(val str: String)

class MyClass {
    context(a: MyValueClass)
    fun Boolean.contextAndReceiverAndValue(param: Long) {}

    context(a: MyValueClass)
    val Boolean.propertyContextReceiver: Int get() = 0
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: MyClass.class[contextAndReceiverAndValue;propertyContextReceiver]
// LIGHT_ELEMENTS_NO_DECLARATION: MyClass.class[contextAndReceiverAndValue-6vsufiI;getPropertyContextReceiver--12Meu8]
