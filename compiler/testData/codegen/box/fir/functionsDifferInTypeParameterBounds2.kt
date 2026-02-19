open class A
open class B
open class C

interface X {
    fun <T1, T2, T3> foo(t1: T1, t2: T2, t3: T3): String
    fun <T1, T2 : B, T3> foo(t1: T1, t2: T2, t3: T3): String
    fun <T1 : C, T2, T3 : A> foo(t1: T1, t2: T2, t3: T3): String
    fun <T1, T2 : A, T3 : B> foo(t1: T1, t2: T2, t3: T3): String

    fun <T11 : A, T12 : B, T13 : C> foo(t1: T11, t2: T12, t3: T13): String
    fun <T21 : A, T22 : C, T23 : B> foo(t1: T21, t2: T22, t3: T23): String
    fun <T31 : B, T32 : A, T33 : C> foo(t1: T31, t2: T32, t3: T33): String
    fun <T41 : B, T42 : C, T43 : A> foo(t1: T41, t2: T42, t3: T43): String
    fun <T51 : C, T52 : A, T53 : B> foo(t1: T51, t2: T52, t3: T53): String
    fun <T61 : C, T62 : B, T63 : A> foo(t1: T61, t2: T62, t3: T63): String
}

class Y : X {
    override fun <S1, S2, S3> foo(t1: S1, t2: S2, t3: S3): String = "___"
    override fun <S1, S2 : B, S3> foo(t1: S1, t2: S2, t3: S3): String = "_B_"
    override fun <S1 : C, S2, S3 : A> foo(t1: S1, t2: S2, t3: S3): String = "C_A"
    override fun <S1, S2 : A, S3 : B> foo(t1: S1, t2: S2, t3: S3): String = "_AB"

    override fun <S11 : A, S12 : B, S13 : C> foo(t1: S11, t2: S12, t3: S13): String = "ABC"
    override fun <S21 : A, S22 : C, S23 : B> foo(t1: S21, t2: S22, t3: S23): String = "ACB"
    override fun <S31 : B, S32 : A, S33 : C> foo(t1: S31, t2: S32, t3: S33): String = "BAC"
    override fun <S41 : B, S42 : C, S43 : A> foo(t1: S41, t2: S42, t3: S43): String = "BCA"
    override fun <S51 : C, S52 : A, S53 : B> foo(t1: S51, t2: S52, t3: S53): String = "CAB"
    override fun <S61 : C, S62 : B, S63 : A> foo(t1: S61, t2: S62, t3: S63): String = "CBA"
}

fun box(): String {
    val a = A()
    val b = B()
    val c = C()
    val y = Y()

    if (y.foo("_", "_", "_") != "___") return "Fail ___"
    if (y.foo("_", b, "_") != "_B_") return "Fail _B_"
    if (y.foo(c, "_", a) != "C_A") return "Fail C_A"
    if (y.foo("_", a, b) != "_AB") return "Fail _AB"

    if (y.foo(a, b, c) != "ABC") return "Fail ABC"
    if (y.foo(a, c, b) != "ACB") return "Fail ACB"
    if (y.foo(b, a, c) != "BAC") return "Fail BAC"
    if (y.foo(b, c, a) != "BCA") return "Fail BCA"
    if (y.foo(c, a, b) != "CAB") return "Fail CAB"
    if (y.foo(c, b, a) != "CBA") return "Fail CBA"

    return "OK"
}
