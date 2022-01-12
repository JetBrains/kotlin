// LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +SealedInlineClasses
// SKIP_TXT

sealed inline class IC

sealed inline class ICC: IC()

inline class ICIC(val a: Any?): IC()