// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -OperatorCreateDelegate

class WrongDelegate(val x: Int) {
    operator fun getValue(thisRef: Any?, prop: Any): Int = x
}

<!UNSUPPORTED_FEATURE(only available since Kotlin 1.1: operator createDelegate)!>operator<!> fun String.createDelegate(thisRef: Any?, prop: Any) = WrongDelegate(this.length)

operator fun String.getValue(thisRef: Any?, prop: Any) = this

val test1: String by "OK"
val test2: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>"OK"<!>
val test3 by "OK"