// ISSUE: KT-77170
// WITH_STDLIB
// WITH_REFLECT

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
