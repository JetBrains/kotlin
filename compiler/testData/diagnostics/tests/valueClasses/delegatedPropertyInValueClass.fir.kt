// !LANGUAGE: +InlineClasses

package kotlin

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
    val testVal by Val()
    var testVar by Var()

    val testValBySingleton by ValObject
    var testVarBySingleton by VarObject
}