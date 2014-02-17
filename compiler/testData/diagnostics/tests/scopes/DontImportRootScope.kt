// FILE: rootPackage.kt
class Klass {
}

fun function() = ""

val property = ""

// FILE: anotherFromRootPackage.kt
fun foo() {
    Klass()
    function() + property
}

// FILE: anotherFromRootPackage.kt
package pkg

import <!UNRESOLVED_REFERENCE!>Klass<!>
import <!UNRESOLVED_REFERENCE!>function<!>
import <!UNRESOLVED_REFERENCE!>property<!>

fun foo() {
    <!UNRESOLVED_REFERENCE!>Klass<!>()
    <!UNRESOLVED_REFERENCE!>function<!>() <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> <!UNRESOLVED_REFERENCE!>property<!>
}
