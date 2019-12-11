//KT-1738 Make it possible to define visibility for constructor parameters which become properties

package kt1738

class A(private var i: Int, var j: Int) {
}

fun test(a: A) {
    a.<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>i<!><!UNRESOLVED_REFERENCE!>++<!>
    a.j++
}