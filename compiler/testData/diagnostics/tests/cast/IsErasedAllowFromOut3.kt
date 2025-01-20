// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
open class Base
class Derived<E : CharSequence> : Base()
fun f(entry: Base) = entry is Derived<out CharSequence>
