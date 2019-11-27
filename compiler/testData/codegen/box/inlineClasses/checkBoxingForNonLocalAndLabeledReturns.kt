// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class ULong(val l: Long)

fun nonLocal(): ULong? {
    val u1 = ULong(1)

    run {
        return u1 // box
    }

    ULong(-1)
}

fun foo(): Boolean = true

fun labeled(): ULong? {
    val u = ULong(2)
    return run {
        if (foo()) return@run u
        ULong(-1) // box
    }
}

fun box(): String {
    if (nonLocal()!!.l != 1L) return "fail"
    if (labeled()!!.l != 2L) return "fail"
    return "OK"
}
