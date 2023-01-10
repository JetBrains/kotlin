// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package kotlin.jvm

annotation class JvmInline

sealed <!SEALED_INLINE_CLASS_WRONG_MODIFIER!>inline<!> class SIC

<!WRONG_MODIFIER_TARGET!>inline<!> object IO

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
<!VALUE_OBJECT_NOT_SEALED_INLINE_CHILD!>value<!> object JVO
