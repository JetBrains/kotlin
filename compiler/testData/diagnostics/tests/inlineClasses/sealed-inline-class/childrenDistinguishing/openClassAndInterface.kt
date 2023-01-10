// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

interface Parent

open class Child: Parent

open class Other

@JvmInline
sealed value class Parent_Child {
    @JvmInline
    value class First(val a: Parent): Parent_Child()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Child<!>): Parent_Child()
}

@JvmInline
sealed value class Other_Parent {
    @JvmInline
    value class First(val a: Other): Other_Parent()

    @JvmInline
    value class Second(val a: Parent): Other_Parent()
}
