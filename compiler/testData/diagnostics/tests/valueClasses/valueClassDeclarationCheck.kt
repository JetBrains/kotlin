// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin

annotation class JvmInline

@JvmInline
value class A0(val x: Int)

@JvmInline
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS!>value<!> class A1
@JvmInline
value class A2<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
@JvmInline
value class A3(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
@JvmInline
value class A4(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>)
@JvmInline
value class A5<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>
@JvmInline
value class A6<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(x: Int, val y: Int)<!>
@JvmInline
value class A7(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg val x: Int<!>)
@JvmInline
value class A8(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>)
@JvmInline
value class A9(final val x: Int)

class B1 {
    companion object {
        @JvmInline
        <!INLINE_CLASS_NOT_TOP_LEVEL!>value<!> class C1(val x: Int)
    }

    @JvmInline
    <!INLINE_CLASS_NOT_TOP_LEVEL!>value<!> class C2(val x: Int)
}

object B2 {
    @JvmInline
    <!INLINE_CLASS_NOT_TOP_LEVEL!>value<!> class C3(val x: Int)
}

@JvmInline
final value class D0(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>open<!> value class D1(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>abstract<!> value class D2(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>sealed<!> value class D3(val x: Int)

<!INCOMPATIBLE_MODIFIERS, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class D4(val x: String)
