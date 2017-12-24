// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Delegate<T>

operator fun Delegate<*>.getValue(receiver: Any?, p: Any): String = ""
operator fun <T> Delegate<T>.setValue(receiver: Any?, p: Any, value: T) {}

operator fun <T> String.provideDelegate(receiver: Any?, p: Any) = Delegate<T>()

var test1: String by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Delegate<!>()
var test2: String by Delegate<String>()

var test3: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_MISSING, DELEGATE_SPECIAL_FUNCTION_MISSING!>"OK"<!>

var test4: String by "OK".<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>provideDelegate<!>(null, "")
var test5: String by "OK".provideDelegate<String>(null, "")