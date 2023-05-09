// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-57502

import kotlin.properties.Delegates

fun test(foo: Any) {
    var test by Delegates.observable(true) { property, oldValue, newValue ->  }
    test = foo is String
    if (test) {
        foo.<!UNRESOLVED_REFERENCE!>length<!> // no smartcast
    }
}
