// one.ValueClassAsContextParameterKt
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class MyValueClass(val str: String)

context(a: MyValueClass)
fun Boolean.contextAndReceiverAndValue(param: Long) {}

context(a: MyValueClass)
val Boolean.propertyContextReceiver: Int get() = 0
