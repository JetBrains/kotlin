// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package kotlin.jvm

annotation class JvmInline

sealed inline class SIC

inline object IO

@JvmInline
value object JVO