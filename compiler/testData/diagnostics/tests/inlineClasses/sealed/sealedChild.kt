// LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +SealedInlineClasses
// SKIP_TXT

sealed inline class IC

sealed inline class ICC: IC()

inline class ICIC(val a: <!INLINE_CLASS_UNDERLYING_VALUE_IS_SUBCLASS_OF_ANOTHER_UNDERLYING_VALUE!>Any?<!>): IC()
