// CHECK_BYTECODE_LISTING
// WITH_STDLIB

enum class E { X }

object O {
    val E.d: Delegate get() = Delegate()
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any?) =
        if (thisRef == null) "OK" else "Failed"
}

fun box(): String = with(O) {
    val result by E.X.d
    result
}
