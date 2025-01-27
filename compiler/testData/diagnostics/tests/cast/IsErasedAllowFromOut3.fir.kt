// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
open class Base
class Derived<E : CharSequence> : Base()
fun f(entry: Base) = entry is <!CANNOT_CHECK_FOR_ERASED_DEPRECATION_WARNING!>Derived<out CharSequence><!>
