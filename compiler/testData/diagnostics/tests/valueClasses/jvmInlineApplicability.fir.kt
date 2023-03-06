// !SKIP_JAVAC
// !LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
inline class IC(val a: Any)

@JvmInline
value class VC(val a: Any)

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
class C

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
interface I

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
object O

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
data class DC(val a: Any)
