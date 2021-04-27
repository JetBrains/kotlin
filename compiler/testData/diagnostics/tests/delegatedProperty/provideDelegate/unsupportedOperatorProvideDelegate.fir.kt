// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -OperatorProvideDelegate

class WrongDelegate(val x: Int) {
    operator fun getValue(thisRef: Any?, prop: Any): Int = x
}

operator fun String.provideDelegate(thisRef: Any?, prop: Any) = WrongDelegate(this.length)

operator fun String.getValue(thisRef: Any?, prop: Any) = this

val test1: String by <!TYPE_MISMATCH!>"OK"<!>
val test2: Int by "OK"
val test3 by "OK"
