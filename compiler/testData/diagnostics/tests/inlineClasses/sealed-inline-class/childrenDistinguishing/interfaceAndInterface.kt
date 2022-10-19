// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

interface One

interface Other

@JvmInline
sealed value class One_Other {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>One<!>): One_Other()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Other<!>): One_Other()
}
