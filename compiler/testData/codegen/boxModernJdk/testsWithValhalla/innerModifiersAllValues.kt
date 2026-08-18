// VALHALLA_SUPPORT: ALL_VALUES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// CHECK_BYTECODE_TEXT

// FILE: Outer.java
public class Outer {
    public static value record V(int x) {}
    public static record NV(int x) {}
}

// FILE: test.kt

class A {
    @JvmField
    val instance = "OK"
    companion object {
        @JvmField
        val static = "OK"
    }
    class B(val x: Char)
    open class C(val x: Char)
    abstract class D(val x: Char)
    inner class E(val x: Char)
    interface F {
        val x: Char
        companion object {
            @JvmField
            val static = "OK"
        }
    }
    annotation class G(val x: Char)
    enum class H { OK }
    @JvmInline
    value class I(val x: Char)
    value class J(val x: Char)
}

fun box(): String {
    require(A.B('a').x == 'a')
    require(A.C('a').x == 'a')
    require(object : A.D('a') {}.x == 'a')
    require(A().E('a').x == 'a')
    require(object : A.F { override val x: Char get() = 'a' }.x == 'a')
    require(A().instance == "OK")
    require(A.static == "OK")
    require(A.F.static == "OK")
    require(A.I('a').x == 'a')
    require(A.J('a').x == 'a')
    return "OK"
}

class UseV(val v: Outer.V)
class UseNV(val v: Outer.NV)

// 3 public final static synchronized INNERCLASS A\$B A B
// 3 public static synchronized INNERCLASS A\$C A C
// 3 public static synchronized abstract INNERCLASS A\$D A D
// 3 public final synchronized INNERCLASS A\$E A E
// 5 public static abstract INNERCLASS A\$F A F
// 2 public static abstract INNERCLASS A\$G A G
// 2 public final static synchronized enum INNERCLASS A\$H A H
// 3 public final static INNERCLASS A\$I A I
// 3 public final static INNERCLASS A\$J A J
// 1 public final static INNERCLASS Outer\$V Outer V
// 1 public final static synchronized INNERCLASS Outer\$NV Outer NV
