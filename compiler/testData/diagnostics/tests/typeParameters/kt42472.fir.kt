// WITH_REFLECT

import kotlin.reflect.KProperty

fun interface ReadOnlyProperty<in T, out V> {
    operator fun getValue(thisRef: T, property: KProperty<*>): V
}

class Problem {
    val variable: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH, NEW_INFERENCE_ERROR, NEW_INFERENCE_ERROR!>delegate()<!> // delegate returns `ReadOnlyProperty<Problem, {CharSequence & Int}>`
    fun <T : CharSequence> delegate() = null as ReadOnlyProperty<Problem, T>
}
