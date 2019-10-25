// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    foo(String::extensionReceiver)
    foo(::valueParameter)
}

fun CharSequence.extensionReceiver(): CharSequence = TODO()
fun String.extensionReceiver(): String = TODO()

fun valueParameter(c: CharSequence): CharSequence = TODO()
fun valueParameter(s: String): CharSequence = TODO()

fun <R> foo(f: (String) -> R) {}
