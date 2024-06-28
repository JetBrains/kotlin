package test

import test.SamePackage as Aliased
import test.samePackage as aliased

class SamePackage

fun samePackage() {}

fun usage(a: SamePackage) {
    samePackage()
}