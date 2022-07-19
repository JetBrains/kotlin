// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class Top {
    @JvmInline
    sealed value class Middle: Top()
    @JvmInline
    value class Child(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>String<!>): Top()
}