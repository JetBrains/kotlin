class A<T>

fun Any.extensionApplicable1() {}

fun A<Int>.extensionApplicable2() {}

fun String.extensionWrongReceiver1() {}

fun A<String>.extensionWrongReceiver2() {}

fun <T> T.extensionWithTypeParameter1(): T = TODO()

fun <T> A<T>.extensionWithTypeParameter2(p: A<T>): T = TODO()

fun test(a: A<Int>) {
    fun Any.localExtensionApplicable() {}

    a::<caret><caret_onAirContext>extension
}