// !DIAGNOSTICS: -UNUSED_PARAMETER
var a: Int by A()
var a1 by <!DELEGATE_SPECIAL_FUNCTION_MISSING, DELEGATE_SPECIAL_FUNCTION_MISSING!>A()<!>

var b: Int by B()

val cObj = C()
var c: String by cObj

class A {
  fun <T> getValue(t: Any?, p: PropertyMetadata): T = null!!
  fun <T> setValue(t: Any?, p: PropertyMetadata, x: T) = Unit
}

class B

fun <T> B.getValue(t: Any?, p: PropertyMetadata): T = null!!
fun <T> B.setValue(t: Any?, p: PropertyMetadata, x: T) = Unit

class C

inline fun <reified T> C.getValue(t: Any?, p: PropertyMetadata): T = null!!
inline fun <reified T> C.setValue(t: Any?, p: PropertyMetadata, x: T) = Unit
