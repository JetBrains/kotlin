// !LANGUAGE: -InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin

annotation class JvmInline

<!UNSUPPORTED_FEATURE, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>value<!> annotation class InlineAnn
<!WRONG_MODIFIER_TARGET!>value<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>value<!> enum class InlineEnum

@JvmInline
<!UNSUPPORTED_FEATURE!>value<!> class NotVal(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)