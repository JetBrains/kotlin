// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

open class Parent

class Child: Parent()

@JvmInline
sealed value class Parent_Child {
    @JvmInline
    value class First(val a: Parent): Parent_Child()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: Child): Parent_Child()
}

@JvmInline
sealed value class String_Parent {
    @JvmInline
    value class First(val a: String): String_Parent()

    @JvmInline
    value class Second(val a: Parent): String_Parent()
}
