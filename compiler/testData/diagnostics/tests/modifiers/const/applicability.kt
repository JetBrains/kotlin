// !DIAGNOSTICS:-UNUSED_VARIABLE,-CAST_NEVER_SUCCEEDS

import kotlin.reflect.KProperty

const val topLevel: Int = 0
const val topLevelInferred = 1
<!WRONG_MODIFIER_TARGET!>const<!> var topLeveLVar: Int = 2

private val privateTopLevel = 3

object A {
    const val inObject: Int = 4
}

class B(<!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val constructor: Int = 5)

abstract class C {
    <!INCOMPATIBLE_MODIFIERS!>open<!> <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, INCOMPATIBLE_MODIFIERS!>const<!> val x: Int = 6

    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, INCOMPATIBLE_MODIFIERS!>const<!> val y: Int = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>7<!>

    companion object {
        const val inCompaionObject = 8
    }
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>object D<!> : C() {
    <!INCOMPATIBLE_MODIFIERS!>override<!> <!INCOMPATIBLE_MODIFIERS!>const<!> val x: Int = 9

    const val inObject = 10

    final const val final = 11

    <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val withoutInitializer: Int

    init {
        withoutInitializer = 12
    }
}

const val delegated: Int <!CONST_VAL_WITH_DELEGATE!>by Delegate()<!>


const val withGetter: Int
<!CONST_VAL_WITH_GETTER!>get() = 13<!>

const val withExplicitDefaultGetter: Int = 1
    <!CONST_VAL_WITH_GETTER!>get<!>

fun foo(): Int {
    <!WRONG_MODIFIER_TARGET!>const<!> val local: Int = 14
    return 15
}

enum class MyEnum {
    A {
        <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val inEnumEntry = 16
    };
    <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val inEnum = 17
}

class Outer {
    inner class Inner {
        object C {
            const val a = 18
        }
    }
}

const val defaultGetter = 19
    <!CONST_VAL_WITH_GETTER!>get<!>

const val nonConstInitializer1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>foo()<!>
const val nonConstInitializer2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 as String<!>
const val nonConstInitializer3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1.0 as String<!>
const val nonConstInitializer4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1 as Double<!>
const val nonConstInitializer5 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"2" as Int<!>

// ------------------
class Delegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): Int = 1

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Int) = Unit
}
