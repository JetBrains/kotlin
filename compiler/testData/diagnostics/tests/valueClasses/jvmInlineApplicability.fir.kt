// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

@JvmInline
inline class IC(val a: Any)

@JvmInline
value class VC(val a: Any)

@JvmInline
class C

@JvmInline
interface I

@JvmInline
object O

@JvmInline
data class DC(val a: Any)
