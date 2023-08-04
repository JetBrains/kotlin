// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-61077
class Delegate<T>

operator fun Delegate<*>.getValue(receiver: Any?, p: Any): String = ""
operator fun <T> Delegate<T>.setValue(receiver: Any?, p: Any, value: T) {}

operator fun <T> String.provideDelegate(receiver: Any?, p: Any) = Delegate<T>()

var test1: String by Delegate()
var test2: String by Delegate<String>()

var test3: String by "OK"

var test4: String by "OK".provideDelegate(null, "")
var test5: String by "OK".provideDelegate<String>(null, "")
