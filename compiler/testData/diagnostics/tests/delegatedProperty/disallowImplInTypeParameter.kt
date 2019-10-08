import kotlin.reflect.KProperty0

val a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>A()<!>

class A {
    fun getValue(<!UNUSED_PARAMETER!>t<!>: Any?, <!UNUSED_PARAMETER!>p<!>: KProperty0<*>): Int = 1
}
