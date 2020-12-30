// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class A0(val x: Int)

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS!>inline<!> class A1
inline class A2<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>()<!>
inline class A3(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
inline class A4(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>)
inline class A5<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>
inline class A6<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(x: Int, val y: Int)<!>
inline class A7(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>vararg val x: Int<!>)
inline class A8(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val x: Int<!>)
inline class A9(final val x: Int)

class B1 {
    companion object {
        inline class C1(val x: Int)
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> inline class C11(val x: Int)
    }

    inline class C2(val x: Int)
    inner <!INLINE_CLASS_NOT_TOP_LEVEL!>inline<!> class C21(val x: Int)
}

object B2 {
    inline class C3(val x: Int)
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> inline class C31(val x: Int)
}

fun foo() {
    <!INLINE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>inline<!> class C4(val x: Int)
}

final inline class D0(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>open<!> inline class D1(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>abstract<!> inline class D2(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>sealed<!> inline class D3(val x: Int)

<!INCOMPATIBLE_MODIFIERS!>inline<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class <!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>D4(val x: String)<!>
