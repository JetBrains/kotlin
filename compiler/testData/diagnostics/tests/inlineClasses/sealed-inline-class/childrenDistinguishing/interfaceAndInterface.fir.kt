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
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: One): One_Other()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: Other): One_Other()
}
