// FIR_IDENTICAL
// KT-61051 K1/K2 difference on extension functions with specific extension receiver types when compiling code that has itself as a dependency
// MODULE: lib1
// FILE: main.kt
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

// MODULE: lib2(lib1)
// FILE: main.kt
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
