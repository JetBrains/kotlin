    // FILE: DeepNestingHeader.kt
package ru.spbau.mit.declaration

typealias BiIntPredicate = (Int, Int) -> Boolean

object TopLevelObject {
    class NestedInsideTopLevelObject
    object ObjectInsideTopLevelObject {
        val objectInsideTopLevelObjectProp = 42
    }

    val topLevelObjectProp = 1
    fun topLevelObjectFoo() {}
}

class Outer {
    inner class InnerInsideOuter {
        inner class InnerInsideInner
    }

    class NestedInsideOuter {
        class NestedInsideNested
        inner class InnerInsideNested
        object ObjectInsideNested {
            val objectInsideNestedProp = 42
        }

        companion object NamedCompanionFromNested {
            val namedCompanionObjectInsideNestedProp = 42
        }
    }

    companion object {
        class NestedInsideOuterNonameCompanion
        object ObjectInsideOuterNonameCompanion {
            val objectOuterNonameCompanionProp = 42
        }

        val companionProp = 1
        fun companionNonameFoo() {}
    }

    object NamedObjectInsideOuter {
        class NestedInsideOuterObject

        object ObjectInsideOuterNamedObject {
            val objectInsideOuterNamedObjectProp = 42
        }

        val namedObjectProp = 1
        fun namedObjectFoo() {}
    }
}

// FILE: DeepNestingUsage.kt
package ru.spbau.mit

import ru.spbau.mit.declaration.BiIntPredicate
import ru.spbau.mit.declaration.TopLevelObject
import ru.spbau.mit.declaration.TopLevelObject.topLevelObjectProp
import ru.spbau.mit.declaration.TopLevelObject.ObjectInsideTopLevelObject
import ru.spbau.mit.declaration.TopLevelObject.ObjectInsideTopLevelObject.objectInsideTopLevelObjectProp
import ru.spbau.mit.declaration.TopLevelObject.NestedInsideTopLevelObject
import ru.spbau.mit.declaration.Outer
import ru.spbau.mit.declaration.Outer.InnerInsideOuter
import ru.spbau.mit.declaration.Outer.InnerInsideOuter.InnerInsideInner
import ru.spbau.mit.declaration.Outer.NestedInsideOuter
import ru.spbau.mit.declaration.Outer.NestedInsideOuter.ObjectInsideNested
import ru.spbau.mit.declaration.Outer.NestedInsideOuter.NestedInsideNested
import ru.spbau.mit.declaration.Outer.NestedInsideOuter.NamedCompanionFromNested
import ru.spbau.mit.declaration.Outer.NestedInsideOuter.InnerInsideNested
// Импорты с companion нужно замьютить, смотреть на декларации с именем класса в префиксе?
import ru.spbau.mit.declaration.Outer.Companion.NestedInsideOuterNonameCompanion
import ru.spbau.mit.declaration.Outer.Companion.companionProp
import ru.spbau.mit.declaration.Outer.Companion.companionNonameFoo
import ru.spbau.mit.declaration.Outer.Companion.ObjectInsideOuterNonameCompanion
import ru.spbau.mit.declaration.Outer.Companion.ObjectInsideOuterNonameCompanion.objectOuterNonameCompanionProp
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter.namedObjectFoo
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter.namedObjectProp
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter.ObjectInsideOuterNamedObject
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter.ObjectInsideOuterNamedObject.objectInsideOuterNamedObjectProp
import ru.spbau.mit.declaration.Outer.NamedObjectInsideOuter.NestedInsideOuterObject

fun aliasArgApplier(body: BiIntPredicate) = body(4, 4)

fun box(): String {

    val topLevelObjectProp1 = topLevelObjectProp
    val objectInsideTopLevelObjectProp1 = objectInsideTopLevelObjectProp
    val nestedInsideTopLevelObject = NestedInsideTopLevelObject()
    val outer = Outer()
    val innerInsideOuter = outer.InnerInsideOuter()
    val innerInsideInner = innerInsideOuter.InnerInsideInner()
    val nestedInsideOuter = NestedInsideOuter()
    val objectInsideNestedProp = ObjectInsideNested.objectInsideNestedProp
    val nestedInsideNested = NestedInsideNested()

    return if (aliasArgApplier { i, i2 -> i == i2 }) "OK" else "Fail"

}
