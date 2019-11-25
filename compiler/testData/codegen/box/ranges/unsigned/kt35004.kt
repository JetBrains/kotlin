// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun ULong.foobar() =
    when (this) {
        in 0U..1U -> "OK"
        else -> throw AssertionError("$this")
    }

fun box(): String = 0UL.foobar()