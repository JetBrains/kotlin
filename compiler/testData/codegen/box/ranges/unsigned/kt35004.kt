// WITH_STDLIB

fun ULong.foobar() =
    when (this) {
        in 0U..1U -> "OK"
        else -> throw AssertionError("$this")
    }

fun box(): String = 0UL.foobar()