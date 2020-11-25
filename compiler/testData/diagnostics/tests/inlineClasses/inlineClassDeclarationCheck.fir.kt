// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class A0(val x: Int)

inline class A1
inline class A2()
inline class A3(x: Int)
inline class A4(var x: Int)
inline class A5(val x: Int, val y: Int)
inline class A6(x: Int, val y: Int)
inline class A7(vararg val x: Int)
inline class A8(open val x: Int)
inline class A9(final val x: Int)

class B1 {
    companion object {
        inline class C1(val x: Int)
        inner inline class C11(val x: Int)
    }

    inline class C2(val x: Int)
    inner inline class C21(val x: Int)
}

object B2 {
    inline class C3(val x: Int)
    inner inline class C31(val x: Int)
}

fun foo() {
    inline class C4(val x: Int)
}

final inline class D0(val x: Int)
open inline class D1(val x: Int)
abstract inline class D2(val x: Int)
sealed inline class D3(val x: Int)

<!INCOMPATIBLE_MODIFIERS!>inline<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class D4(val x: String)
