// FILE: rootPackage.kt
class Klass {
    class Nested
}

class NotImported

fun function() = ""

val property = ""

// FILE: anotherFromRootPackage.kt
fun foo(): Klass {
    function() + property
    return Klass()
}

// FILE: yetAnotherFromRootPackage.kt
package pkg

import Klass
import Klass.Nested
import function
import property

fun foo(): Klass {
    function() + property
    return Klass()
}

val v: Nested = Nested()
val x: NotImported = <!UNRESOLVED_REFERENCE!>NotImported<!>()
