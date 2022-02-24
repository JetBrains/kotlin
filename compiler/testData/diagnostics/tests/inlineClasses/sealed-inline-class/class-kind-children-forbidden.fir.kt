// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC
// !DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package kotlin.jvm

annotation class JvmInline

@JvmInline
sealed value class SVC

interface I : SVC()
enum class EC : SVC()
annotation class AC : SVC()