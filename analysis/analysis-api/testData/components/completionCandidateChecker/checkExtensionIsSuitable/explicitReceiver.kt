class A<T>

fun Any.extensionApplicable1() {}

fun A<Int>.extensionApplicable2() {}

fun String.extensionWrongReceiver1() {}

fun A<String>.extensionWrongReceiver2() {}

fun <T> T.extensionWithTypeParameter1(): T = TODO()

fun <T> A<T>.extensionWithTypeParameter2(p: A<T>): T = TODO()

val Any.extensionVariableApplicable: Int get() = 1

val Any.extensionFunctionalVariableApplicable: Any.() -> Unit get() = {}

val Int.extensionFunctionalVariableWrongReceiver: Any.() -> Unit get() = {}

val <T> T.extensionVariableWithTypeParameter: T get() = TODO()

fun test(a: A<Int>, functionalVariableApplicable: Any.() -> Unit) {
    fun Any.localExtensionApplicable() {}

    a.<caret><caret_onAirContext>extension
}