// LANGUAGE: -InlineClasses, -JvmInlineValueClasses
// DIAGNOSTICS: -UNUSED_PARAMETER

inline class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>inline<!> annotation class InlineAnn
<!WRONG_MODIFIER_TARGET!>inline<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>inline<!> enum class InlineEnum

inline class NotVal(<!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
