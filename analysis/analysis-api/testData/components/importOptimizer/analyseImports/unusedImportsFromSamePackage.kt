// FILE: main.kt

package test

import test.SamePackage
import test.samePackage

fun usage(a: SamePackage) {
    samePackage()
}

// FILE: dependency.kt
package test

class SamePackage

fun samePackage() {}
