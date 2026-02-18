// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// Two bugs:
// 1. K1 fails to compute types for 'b19'. New reflect implementation computes the types correctly
// 2. flexibility mismatch for `kotlinInterface2Fun1`. I will fix this bug in the next commits
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: Java.java
public abstract class Java extends KotlinInterface implements KotlinInterface2<Integer> { }

// FILE: main.kt
interface Inv<T>
interface Box1<T> where T : CharSequence?, T : Number
interface Box2<T> where T : Number?, T : CharSequence
interface Box3<T> where T : CharSequence, T : Number?
interface OutBox3<out T> where T : CharSequence, T : Number?
interface InBox3<in T> where T : CharSequence, T : Number?

abstract class KotlinInterface<A00, A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23>
    where A01 : Any,
          A02 : Number?,
          A03 : Number?, A03 : CharSequence,
          A04 : CharSequence, A04 : Number?,
          A05 : CharSequence?, A05 : Number,
          A06 : Inv<A06>,
          A07 : Inv<*>,
          A08 : Inv<A08>?,
          A09 : Inv<*>?,
          A10 : Inv<A10?>?,
          A11 : Inv<A11>?, A11 : Number,
          A12 : Inv<A12>, A12 : Number?,
          A13 : Box1<A13>, A13 : CharSequence, A13 : Number,
          A14 : Box2<A14>, A14 : CharSequence, A14 : Number,
          A15 : Box3<A15>, A15 : CharSequence, A15 : Number,
          A16 : Box3<A16>, A16 : CharSequence?, A16 : Number,
          A17 : Box3<*>,
          A18 : Box3<out A18>, A18 : CharSequence?, A18 : Number,
          A19 : Box3<in A19>, A19 : CharSequence?, A19 : Number,
          A20 : OutBox3<A20>, A20 : CharSequence?, A20 : Number,
          A21 : InBox3<A21>, A21 : CharSequence?, A21 : Number,
          A22 : A17,
          A23 : A18 {
    abstract fun a00(a: A00): A00
    abstract fun a01(a: A01): A01
    abstract fun a02(a: A02): A02
    abstract fun a03(a: A03): A03
    abstract fun a04(a: A04): A04
    abstract fun a05(a: A05): A05
    abstract fun a06(a: A06): A06
    abstract fun a07(a: A07): A07
    abstract fun a08(a: A08): A08
    abstract fun a09(a: A09): A09
    abstract fun a10(a: A10): A10
    abstract fun a11(a: A11): A11
    abstract fun a12(a: A12): A12
    abstract fun a13(a: A13): A13
    abstract fun a14(a: A14): A14
    abstract fun a15(a: A15): A15
    abstract fun a16(a: A16): A16
    abstract fun a17(a: A17): A17
    abstract fun a18(a: A18): A18
    abstract fun a19(a: A19): A19
    abstract fun a20(a: A20): A20
    abstract fun a21(a: A21): A21
    abstract fun a22(a: A22): A22
    abstract fun a23(a: A23): A23

    abstract fun <B00 : A00> b00(a: B00): B00
    abstract fun <B01 : A01> b01(a: B01): B01
    abstract fun <B02 : A02> b02(a: B02): B02
    abstract fun <B03 : A03> b03(a: B03): B03
    abstract fun <B04 : A04> b04(a: B04): B04
    abstract fun <B05 : A05> b05(a: B05): B05
    abstract fun <B06 : A06> b06(a: B06): B06
    abstract fun <B07 : A07> b07(a: B07): B07
    abstract fun <B08 : A08> b08(a: B08): B08
    abstract fun <B09 : A09> b09(a: B09): B09
    abstract fun <B10 : A10> b10(a: B10): B10
    abstract fun <B11 : A11> b11(a: B11): B11
    abstract fun <B12 : A12> b12(a: B12): B12
    abstract fun <B13 : A13> b13(a: B13): B13
    abstract fun <B14 : A14> b14(a: B14): B14
    abstract fun <B15 : A15> b15(a: B15): B15
    abstract fun <B16 : A16> b16(a: B16): B16
    abstract fun <B17 : A17> b17(a: B17): B17
    abstract fun <B18 : A18> b18(a: B18): B18
    abstract fun <B19 : A19> b19(a: B19): B19
    abstract fun <B20 : A20> b20(a: B20): B20
    abstract fun <B21 : A21> b21(a: B21): B21
    abstract fun <B22 : A22> b22(a: B22): B22
    abstract fun <B23 : A23> b23(a: B23): B23

    abstract fun <C00 : Inv<A00>> c00(a: C00): C00
    abstract fun <C01 : Inv<A01>> c01(a: C01): C01
    abstract fun <C02 : Inv<A02>> c02(a: C02): C02
    abstract fun <C03 : Inv<A03>> c03(a: C03): C03
    abstract fun <C04 : Inv<A04>> c04(a: C04): C04
    abstract fun <C05 : Inv<A05>> c05(a: C05): C05
    abstract fun <C06 : Inv<A06>> c06(a: C06): C06
    abstract fun <C07 : Inv<A07>> c07(a: C07): C07
    abstract fun <C08 : Inv<A08>> c08(a: C08): C08
    abstract fun <C09 : Inv<A09>> c09(a: C09): C09
    abstract fun <C10 : Inv<A10>> c10(a: C10): C10
    abstract fun <C11 : Inv<A11>> c11(a: C11): C11
    abstract fun <C12 : Inv<A12>> c12(a: C12): C12
    abstract fun <C13 : Inv<A13>> c13(a: C13): C13
    abstract fun <C14 : Inv<A14>> c14(a: C14): C14
    abstract fun <C15 : Inv<A15>> c15(a: C15): C15
    abstract fun <C16 : Inv<A16>> c16(a: C16): C16
    abstract fun <C17 : Inv<A17>> c17(a: C17): C17
    abstract fun <C18 : Inv<A18>> c18(a: C18): C18
    abstract fun <C19 : Inv<A19>> c19(a: C19): C19
    abstract fun <C20 : Inv<A20>> c20(a: C20): C20
    abstract fun <C21 : Inv<A21>> c21(a: C21): C21
    abstract fun <C22 : Inv<A22>> c22(a: C22): C22
    abstract fun <C23 : Inv<A23>> c23(a: C23): C23

    abstract fun <D00 : Box1<A05>> d00(a: D00): D00
    abstract fun <D01 : Box1<A05>> d01(a: D01): D01
    abstract fun <D02 : Box1<A05>> d02(a: D02): D02
    abstract fun <D03 : Box1<A05>> d03(a: D03): D03
    abstract fun <D04 : Box1<A05>> d04(a: D04): D04
    abstract fun <D05 : Box1<A05>> d05(a: D05): D05
    abstract fun <D06 : Box1<A05>> d06(a: D06): D06
    abstract fun <D07 : Box1<A05>> d07(a: D07): D07
    abstract fun <D08 : Box1<A05>> d08(a: D08): D08
    abstract fun <D09 : Box1<A05>> d09(a: D09): D09
    abstract fun <D10 : Box1<A05>> d10(a: D10): D10
    abstract fun <D11 : Box1<A05>> d11(a: D11): D11
    abstract fun <D12 : Box1<A05>> d12(a: D12): D12
    abstract fun <D13 : Box1<A05>> d13(a: D13): D13
    abstract fun <D14 : Box1<A05>> d14(a: D14): D14
    abstract fun <D15 : Box1<A05>> d15(a: D15): D15
    abstract fun <D16 : Box1<A05>> d16(a: D16): D16
    abstract fun <D17 : Box1<A05>> d17(a: D17): D17
    abstract fun <D18 : Box1<A05>> d18(a: D18): D18
    abstract fun <D19 : Box1<A05>> d19(a: D19): D19
    abstract fun <D20 : Box1<A05>> d20(a: D20): D20
    abstract fun <D21 : Box1<A05>> d21(a: D21): D21
    abstract fun <D22 : Box1<A05>> d22(a: D22): D22
    abstract fun <D23 : Box1<A05>> d23(a: D23): D23

    abstract fun <A> normalFun1(a: A): A
    abstract fun <A : Any> normalFun2(a: A): A
    abstract fun <A : Inv<A>> normalFun3(a: A): A
    abstract fun <A : Inv<A>> normalFun4(a: A, b: Inv<Int>): Inv<Int>
    abstract fun <A, B : Inv<A>> normalFun5(a: A, b: B)
    abstract fun <A : Any, B : Inv<A>> normalFun6(a: A, b: B)
    inline fun <reified A : Any, B : Inv<A>> normalFun7(a: A, b: B) = Unit
    inline fun <A : Any, reified B : Inv<A>> normalFun8(a: A, b: B) = Unit
}

interface KotlinInterface2<A> {
    fun kotlinInterface2Fun1(a: A): A
    fun <B> kotlinInterface2Fun2(a: B): B
}
