// FILE: InnerNestedDeclarations.kt
package ru.spbau.mit

object TopLevelObject {
    val prop = 1
    fun topLevelFoo() {}
}

class Outer {
    inner class Inner {
    }

    class Nested {
    }

    companion object {
        val companionProp = 1
        fun companionNonameFoo() {}
    }

    object NamedObject {
        val namedObjectProp = 1
        fun namedObjectFoo() {}
    }
}

class WithNamedCompanion {
    companion object {
        val namedCompanionProp = 1
        fun companionNamedFoo() {}
    }
}
// FILE: InnerNestedInvocations.kt
package ru.spbau.mit

import ru.spbau.mit.Outer
import ru.spbau.mit.Outer.Inner
import ru.spbau.mit.Outer.Nested

fun box(): String {
    val outer: Outer = Outer()
    val nested: Nested = Nested()
    val inner: Inner = outer.Inner()
    val companionProp: Int = Outer.companionProp
    val prop: Int = TopLevelObject.prop
    val namedCompanionProp: Int = WithNamedCompanion.namedCompanionProp
    WithNamedCompanion.companionNamedFoo()
    Outer.companionNonameFoo()
    TopLevelObject.topLevelFoo()
    Outer.NamedObject.namedObjectFoo()
    return "OK"
}
