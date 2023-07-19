// FILE: main.kt

package test

import test.SamePackage
import test.SamePackage as Aliased
import test.samePackage
import test.samePackage as aliased

fun usage(a: Aliased, b: SamePackage) {
    aliased()
    samePackage()
}

// FILE: dependency.kt
package test

class SamePackage

fun samePackage() {}
