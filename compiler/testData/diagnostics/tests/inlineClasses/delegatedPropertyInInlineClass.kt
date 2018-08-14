// !LANGUAGE: +InlineClasses

class Val {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 1
}

class Var {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 2
    operator fun setValue(thisRef: Any?, kProp: Any?, value: Int) {}
}


object ValObject {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 1
}

object VarObject {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 2
    operator fun setValue(thisRef: Any?, kProp: Any?, value: Int) {}
}

inline class Z(val data: Int) {
    val testVal <!DELEGATED_PROPERTY_INSIDE_INLINE_CLASS!>by Val()<!>
    var testVar <!DELEGATED_PROPERTY_INSIDE_INLINE_CLASS!>by Var()<!>

    val testValBySingleton <!DELEGATED_PROPERTY_INSIDE_INLINE_CLASS!>by ValObject<!>
    var testVarBySingleton <!DELEGATED_PROPERTY_INSIDE_INLINE_CLASS!>by VarObject<!>
}