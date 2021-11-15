// FIR_IDENTICAL
open class Base
class Derived<E : CharSequence> : Base()
fun f(entry: Base) = entry is Derived<out CharSequence>
