// ISSUE: KT-77170
// WITH_REFLECT
// LANGUAGE: +IrInlinerBeforeKlibSerialization

// IGNORE_BACKEND_K2: NATIVE, JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE, JS_IR
// ^^^ KT-77170: IrTypeParameterSymbolImpl is unbound. Signature: [ box.inline.typeParameterSymbolIsUnbound/FFILib.func|func(){0§<kotlin.Function<*>>}[0] <- Local[<TP>,0] ]

// After issues are fixed, please merge this test with `typeParameterSymbolIsUnbound.kt`

// MODULE: lib
// FILE: lib.kt

import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class FFILib() {
    inline fun <reified T : Function<*>> func() = FuncInfo<T>(typeOf<T>())
}

class FuncDelegate<T>() {
    var cached: T? = null
    public operator fun getValue(thisRef: FFILib, prop: KProperty<*>): T {
        return cached!!
    }
}

class FuncInfo<T>(val type: KType) {
    operator fun provideDelegate(thisRef: FFILib, prop: KProperty<*>) = FuncDelegate<T>()
}

// MODULE: main(lib)
// FILE: main.kt

object Advapi32 : FFILib() {
    val RegCloseKey: (hKey: Int) -> Int by func()
}

fun box() = "OK"
