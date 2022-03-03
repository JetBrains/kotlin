// FIR_IDENTICAL
// !SKIP_JAVAC
// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>value<!> interface InlineInterface
<!WRONG_MODIFIER_TARGET!>value<!> annotation class InlineAnn
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>value<!> enum class InlineEnum
