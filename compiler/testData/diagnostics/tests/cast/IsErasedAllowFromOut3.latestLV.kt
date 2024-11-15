// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
open class Base
class Derived<E : CharSequence> : Base()
fun f(entry: Base) = entry is <!CANNOT_CHECK_FOR_ERASED!>Derived<out CharSequence><!>
