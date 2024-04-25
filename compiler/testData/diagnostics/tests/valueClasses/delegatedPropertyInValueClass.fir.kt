// SKIP_JAVAC
// LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

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

@JvmInline
value class Z(val data: Int) {
    val testVal by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>Val()<!>
    var testVar by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>Var()<!>

    val testValBySingleton by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>ValObject<!>
    var testVarBySingleton by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>VarObject<!>
}
