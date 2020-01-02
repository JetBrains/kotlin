// FULL_JDK

import java.util.stream.*

interface A : MutableCollection<String> {
    override fun removeIf(x: java.util.function.Predicate<in String>) = false
}

fun foo(x: MutableList<String>, y: A) {
    x.<!UNRESOLVED_REFERENCE!>removeIf<!> { <!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>length<!> > 0 }
    y.removeIf { it.length > 0 }
}
