// !DIAGNOSTICS:-UNUSED_VARIABLE,-CAST_NEVER_SUCCEEDS,-DIVISION_BY_ZERO

import kotlin.reflect.KProperty

const val topLevel: Int = 0
const val topLevelInferred = 1
const var topLeveLVar: Int = 2

private val privateTopLevel = 3

object A {
    const val inObject: Int = 4
}

class B(const val constructor: Int = 5)

abstract class C {
    <!INCOMPATIBLE_MODIFIERS!>open<!> <!INCOMPATIBLE_MODIFIERS!>const<!> val x: Int = 6

    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>const<!> val y: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>7<!>

    companion object {
        const val inCompaionObject = 8
    }
}

object D : C() {
    <!INCOMPATIBLE_MODIFIERS!>override<!> <!INCOMPATIBLE_MODIFIERS!>const<!> val x: Int = 9

    const val inObject = 10

    final const val final = 11

    const val withoutInitializer: Int

    init {
        withoutInitializer = 12
    }
}

const val delegated: Int by Delegate()


const val withGetter: Int
    get() = 13

const val withExplicitDefaultGetter: Int = 1
    get

fun foo(): Int {
    const val local: Int = 14
    return 15
}

enum class MyEnum {
    A {
        const val inEnumEntry = 16
    };
    const val inEnum = 17
}

class Outer {
    inner class Inner {
        object C {
            const val a = 18
        }
    }
}

const val defaultGetter = 19
    get

const val nonConstInitializer1 = foo()
const val nonConstInitializer2 = 1 as String
const val nonConstInitializer3 = 1.0 as String
const val nonConstInitializer4 = 1 as Double
const val nonConstInitializer5 = "2" as Int
const val nonConstInitializer6 = 1/0
const val nonConstInitializer7 = -1/0
const val nonConstInitializer8 = 1/0 - 1/0
const val nonConstInitializer9 = 1.0/0.0 - 1/0
const val nonConstInitializer10 = 0/0
const val nonConstInitializer11 = 1 % 0
const val nonConstInitializer12 = 0 % 0
const val nonConstInitializer14 = 0.rem(0)
const val nonConstInitializer15 = 0.div(0)

const val constInitializer1 = 1.0/0
const val constInitializer2 = 1/0.0
const val constInitializer3 = 1.0/0.0
const val constInitializer4 = -1.0/0
const val constInitializer5 = 0.0/0
const val constInitializer6 = 42 + 1.0/0
const val constInitializer7 = 42 - 1.0/0
const val constInitializer8 = 1.0/0 - 1.0/0
const val constInitializer9 = 0.0/0 + 1.0/0
const val constInitializer10 = 1.0 % 0
const val constInitializer11 = 0.0 % 0
const val constInitializer12 = (-1.0) % 0
const val constInitializer13 = 1.0.rem(0)
const val constInitializer15 = 1.0.div(0)

// ------------------
class Delegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): Int = 1

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Int) = Unit
}
