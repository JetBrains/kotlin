// ISSUE: KT-77170
// WITH_REFLECT
// LANGUAGE: -IrInlinerBeforeKlibSerialization

// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ KT-77186: java.lang.NullPointerException	at org.jetbrains.kotlin.ir.backend.js.lower.ClassReferenceLowering.createKTypeProjection(ClassReferenceLowering.kt:212)

// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ KT-76998: Cannot deserialize inline fun: FUN name:func visibility:public modality:FINAL returnType:<root>.FuncInfo<T of <root>.FFILib.func> [inline]
//               TYPE_PARAMETER name:T index:0 variance: superTypes:[kotlin.Function<*>] reified:true
//               VALUE_PARAMETER kind:DispatchReceiver name:<this> index:0 type:<root>.FFILib

// After issues are fixed, please merge this test with `typeParameterSymbolIsUnboundWithInlinedFunInKlib.kt`

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
    public operator fun getValue(thisRef: FFILib, property: KProperty<*>): T {
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
