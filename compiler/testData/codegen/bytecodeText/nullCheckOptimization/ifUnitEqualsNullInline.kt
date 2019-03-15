// IGNORE_BACKEND: JVM_IR
// FILE: test.kt

fun test1(): String {
    val u = Unit
    return u.mapNullable({ "X1" }, { "X2" })
}

// @TestKt.class:
// 0 IFNULL
// 0 IFNONNULL
// 1 X1
// 0 X2

// FILE: inline.kt
inline fun <T : Any, R> T?.mapNullable(ifNotNull: (T) -> R, ifNull: () -> R): R =
        if (this == null) ifNull() else ifNotNull(this)
