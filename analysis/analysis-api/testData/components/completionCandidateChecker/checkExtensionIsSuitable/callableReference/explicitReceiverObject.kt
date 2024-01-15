fun Any.extensionApplicable() {}

fun String.extensionWrongReceiver() {}

object O

fun test() {
    O::<caret><caret_onAirContext>extension
}