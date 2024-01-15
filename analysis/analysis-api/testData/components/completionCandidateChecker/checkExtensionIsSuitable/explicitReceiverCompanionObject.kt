fun Any.extensionApplicable() {}

fun String.extensionWrongReceiver() {}

class A {
    companion object {
    }
}

fun test() {
    A.<caret><caret_onAirContext>extension
}