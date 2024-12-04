// FILE: main.kt
package test

import test.SamePackage as Aliased
import test.samePackage as aliased

fun usage(a: Aliased) {
    aliased()
}

// FILE: dependency.kt
package test

class SamePackage

fun samePackage() {}
