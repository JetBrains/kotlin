// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>value<!> interface InlineInterface
<!WRONG_MODIFIER_TARGET!>value<!> annotation class InlineAnn
<!WRONG_MODIFIER_TARGET!>value<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>value<!> enum class InlineEnum
