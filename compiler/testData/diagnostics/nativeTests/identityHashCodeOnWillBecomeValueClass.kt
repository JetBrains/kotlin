// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// OPT_IN: kotlin.ExperimentalValueClassesApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"

    @OptIn(ExperimentalNativeApi::class)
    fun inside() = <!IDENTITY_SENSITIVE_OPERATION_INSIDE_WILL_BECOME_VALUE_CLASS!>identityHashCode()<!>
}

@OptIn(ExperimentalNativeApi::class)
fun test(p: Wrapper, p2: Wrapper?) {
    <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>p.identityHashCode()<!>
    <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>p2.identityHashCode()<!>
    with(p) { <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>identityHashCode()<!> }
}
