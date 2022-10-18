// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

object O {
    object P
    init {
        throw IllegalStateException("O should not be initialized")
    }
}

operator fun O.P.getValue(thisRef: Any?, property: Any?) = if (thisRef == null) "OK" else "Failed"

val result by O.P

fun box(): String = result