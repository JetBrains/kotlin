// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class A0(val x: Int)

@JvmInline
value class A1
@JvmInline
value class A2()
@JvmInline
value class A3(x: Int)
@JvmInline
value class A4(var x: Int)
@JvmInline
value class A5(val x: Int, val y: Int)
@JvmInline
value class A6(x: Int, val y: Int)
@JvmInline
value class A7(vararg val x: Int)
@JvmInline
value class A8(open val x: Int)
@JvmInline
value class A9(final val x: Int)

class B1 {
    companion object {
        @JvmInline
        value class C1(val x: Int)
    }

    @JvmInline
    value class C2(val x: Int)
}

object B2 {
    @JvmInline
    value class C3(val x: Int)
}

@JvmInline
final value class D0(val x: Int)
open value class D1(val x: Int)
abstract value class D2(val x: Int)
sealed value class D3(val x: Int)

value data class D4(val x: String)
