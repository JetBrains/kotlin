// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}

val a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>Delegate()<!>

class Foo {
    val a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>Delegate()<!>
}

fun foo() {
    val a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>Delegate()<!>
}

