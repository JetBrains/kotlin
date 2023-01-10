// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package kotlin.jvm

annotation class JvmInline

sealed inline class SIC

<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD, WRONG_MODIFIER_TARGET!>inline<!> object IO

@JvmInline
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object JVO
