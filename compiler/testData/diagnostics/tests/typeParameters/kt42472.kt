// WITH_REFLECT

import kotlin.reflect.KProperty

fun interface ReadOnlyProperty<in T, out V> {
    operator fun getValue(thisRef: T, property: KProperty<*>): V
}

class Problem {
    val variable: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!TYPE_MISMATCH!>delegate<!>()<!> // delegate returns `ReadOnlyProperty<Problem, {CharSequence & Int}>`
    fun <T : CharSequence> delegate() = null <!CAST_NEVER_SUCCEEDS!>as<!> ReadOnlyProperty<Problem, T>
}
