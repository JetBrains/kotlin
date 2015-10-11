// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<R>() {
    fun <T> getValue(t: Any?, p: PropertyMetadata): T = null!!
    fun <T> setValue(t: Any?, p: PropertyMetadata, x: T) = Unit
}

var a1: Int by <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>A<!>()
var a2: Int by A<String>()

class B<R>() {
    fun <T> getValue(t: Any?, p: PropertyMetadata): T = null!!
    fun setValue(t: Any?, p: PropertyMetadata, x: R) = Unit
}

var b1: Int by B()
var b2: Int by B<Number>()

class C<R>() {
    fun getValue(t: Any?, p: PropertyMetadata): R = null!!
    fun <T> setValue(t: Any?, p: PropertyMetadata, x: T) = Unit
}

var c1: Int by C()
var c2: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>C<Number>()<!>
