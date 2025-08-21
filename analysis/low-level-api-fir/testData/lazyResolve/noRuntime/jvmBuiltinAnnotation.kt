// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ALLOW_KOTLIN_PACKAGE
// EXPECT_BUILTINS_AS_PART_OF_STDLIB

// FILE: anno.kt
package kotlin.internal

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmBuiltin

// FILE: main.kt

@file:kotlin.internal.JvmBuiltin
package kotlin

interface CharSequence {
    operator fun get(index: Int): Char
}

class String : Comparable<String>, CharSequence {
    override fun ge<caret>t(index: Int): Char
}