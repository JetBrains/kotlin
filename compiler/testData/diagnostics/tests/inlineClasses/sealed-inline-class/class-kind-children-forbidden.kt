// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class SVC

interface I : <!INTERFACE_WITH_SUPERCLASS!>SVC<!><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
enum class EC : <!CLASS_IN_SUPERTYPE_FOR_ENUM!>SVC<!>()
annotation class AC : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>SVC()<!>
