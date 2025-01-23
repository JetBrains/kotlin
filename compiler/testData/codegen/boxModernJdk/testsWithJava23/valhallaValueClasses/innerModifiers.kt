// LANGUAGE: +ValhallaValueClasses
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING
// CHECK_BYTECODE_TEXT

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
    return "OK"
}

// 3 public final static synchronized INNERCLASS A\$B A B
// 3 public static synchronized INNERCLASS A\$C A C
// 3 public static synchronized abstract INNERCLASS A\$D A D
// 3 public final synchronized INNERCLASS A\$E A E
// 5 public static abstract INNERCLASS A\$F A F
// 2 public static abstract INNERCLASS A\$G A G
// 2 public final static synchronized enum INNERCLASS A\$H A H
