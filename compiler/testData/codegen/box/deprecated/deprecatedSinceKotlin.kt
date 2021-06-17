// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

package kotlin.test

@JvmName("contentEqualsNullable")
public inline infix fun <T> Array<out T>?.contentEqualsMy(other: Array<out T>?): Boolean {
    return java.util.Arrays.equals(this, other)
}

@Deprecated("Use Kotlin compiler 1.4 to avoid deprecation warning.")
@DeprecatedSinceKotlin(hiddenSince = "1.4")
public inline infix fun <T> Array<out T>.contentEqualsMy(other: Array<out T>): Boolean {
    return this.contentEqualsMy(other)
}


// MODULE: main(lib)
// FILE: B.kt

import kotlin.test.*

fun box(): String {
    val arr = arrayOf(1, 2, 3)
    return if (arr contentEqualsMy arr) "OK" else "fail"
}
