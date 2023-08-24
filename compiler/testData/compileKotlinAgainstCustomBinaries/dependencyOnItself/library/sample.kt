fun kotlin.String.exampleExtensionFunction() {}

class UserKlass
fun <T> T.erroneousExtensionFunction1() {}

fun kotlin.text.Appendable.erroneousExtensionFunction2() {}

fun test(
    exampleReceiver: kotlin.String,
    receiver1: UserKlass,
    receiver2: kotlin.text.Appendable
) {
    exampleReceiver.exampleExtensionFunction()
    receiver1.erroneousExtensionFunction1()
    receiver2.erroneousExtensionFunction2()
}
