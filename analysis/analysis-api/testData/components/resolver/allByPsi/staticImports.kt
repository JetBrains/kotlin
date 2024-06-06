// IGNORE_STABILITY_K1: candidates
// FILE: dependency.kt
package one.two

object TopLevelObject {
    fun foo() {}

    val bar = 1

    object NestedObject {
        fun baz() {}
        var doo = "str"
    }
}

// FILE: main.kt
package another

import one.two.TopLevelObject
import one.two.TopLevelObject.foo
import one.two.TopLevelObject.bar
import one.two.TopLevelObject.NestedObject
import one.two.TopLevelObject.NestedObject.baz
import one.two.TopLevelObject.NestedObject.doo

fun usage() {
    val explicitTopObject = one.two.TopLevelObject
    val importedTopObject = TopLevelObject

    val explicitFoo = TopLevelObject.foo()
    val importedFoo = foo()

    val explicitBar = TopLevelObject.bar
    val importedBar = bar

    val explicitNestedObject = TopLevelObject.NestedObject
    val importedNestedObject = NestedObject

    val explicitBaz = TopLevelObject.NestedObject.baz()
    val explicitNestedBaz = NestedObject.baz()
    val importedBaz = baz()

    val explicitDoo = TopLevelObject.NestedObject.doo
    val explicitNestedDoo = NestedObject.doo
    val importedDoo = doo

    doo = "str"
}