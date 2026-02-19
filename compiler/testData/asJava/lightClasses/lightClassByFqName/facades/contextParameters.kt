// one.ContextParametersKt
// LANGUAGE: +ContextParameters
// SKIP_IDE_TEST
package one

context(a: Int, b: String)
fun Boolean.contextAndReceiverAndValue(param: Long) {}

context(_: Int)
fun unnamedContext() {}

context(a: Int, b: String)
val Boolean.propertyContextReceiver: Int get() = 0

context(_: Int)
val propertyContext: Boolean get() = false
