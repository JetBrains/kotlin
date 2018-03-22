// !LANGUAGE: -InlineClasses

<!UNSUPPORTED_FEATURE!>inline<!> class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>inline<!> annotation class InlineAnn
<!WRONG_MODIFIER_TARGET!>inline<!> object InlineObject
<!WRONG_MODIFIER_TARGET!>inline<!> enum class InlineEnum
