// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        p.parameters
        p.returnType
        p.annotations
        return p.toString()
    }
}

val prop: String by Delegate()

fun box() = if (prop == "val prop: kotlin.String") "OK" else "Fail: $prop"
