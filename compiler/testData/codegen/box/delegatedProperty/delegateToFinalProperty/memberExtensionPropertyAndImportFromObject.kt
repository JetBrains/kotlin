// CHECK_BYTECODE_LISTING
// WITH_STDLIB

import O.d

enum class E { X }

object O {
    val E.d: Delegate get() = Delegate()
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any?) =
        if (thisRef == null) "OK" else "Failed"
}

val result by E.X.d

fun box(): String = result
