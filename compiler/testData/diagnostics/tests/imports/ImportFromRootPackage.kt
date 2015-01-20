// FILE: rootPackage.kt
class Klass {
}

fun function() = ""

val property = ""

// FILE: anotherFromRootPackage.kt
fun foo(): Klass {
    function() + property
    return Klass()
}

// FILE: anotherFromRootPackage.kt
package pkg

import Klass
import function
import property

fun foo(): Klass {
    function() + property
    return Klass()
}
