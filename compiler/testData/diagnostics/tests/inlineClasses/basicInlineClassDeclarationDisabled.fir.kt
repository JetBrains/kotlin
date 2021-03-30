// !LANGUAGE: -InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class Foo(val x: Int)

inline annotation class InlineAnn
inline object InlineObject
inline enum class InlineEnum

inline class NotVal(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
