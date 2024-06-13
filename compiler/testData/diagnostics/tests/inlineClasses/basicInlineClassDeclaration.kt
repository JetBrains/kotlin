// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, -JvmInlineValueClasses

inline class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>inline<!> interface InlineInterface
<!WRONG_MODIFIER_TARGET!>inline<!> annotation class InlineAnn
<!WRONG_MODIFIER_TARGET!>inline<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>inline<!> enum class InlineEnum
