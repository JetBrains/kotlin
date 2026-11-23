// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

@WillBecomeValue
data class Value(val p: <!WILL_BECOME_VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>dynamic<!>)

@WillBecomeValue
sealed class SValue(p: dynamic, x: dynamic)

@WillBecomeValue
abstract class AValue(q: dynamic) : SValue(q, q) {
    abstract val r: dynamic
}
