// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// Only JVM preview features are enabled here, not Valhalla value classes, so every Kotlin class below is an identity class and
// has `ACC_IDENTITY` in its `InnerClasses` entries (unlike the Java value record `Outer.V`). Without it, the JVM would reject the
// open nested class `A.C` as an illegal value class, and reflection would report the other nested classes as value classes.

// FILE: Outer.java
public class Outer {
    public static value record V(int x) {}
    public static record NV(int x) {}
}

// FILE: test.kt
import java.lang.ref.WeakReference

class A {
    class B(val x: Char)
    open class C(val x: Char)
    abstract class D(val x: Char)
    inner class E(val x: Char)
    interface F {
        val x: Char
    }
    enum class H { OK }
    @JvmInline
    value class I(val x: Char)
    object O
    companion object
}

class UseV(val v: Outer.V)
class UseNV(val v: Outer.NV)

fun box(): String {
    class Local
    val identityObjects = listOf<Any>(
        A.B('a'), A.C('a'), object : A.D('a') {}, A().E('a'), object : A.F { override val x: Char get() = 'a' }, A.H.OK, A.I('a'),
        A.O, A.Companion, Local(),
    )
    for (o in identityObjects) {
        if (o.javaClass.isValue) return "${o.javaClass} must not be a value class"
        WeakReference(o)
    }
    return "OK"
}

// 3 public final static synchronized INNERCLASS A\$B A B
// 3 public static synchronized INNERCLASS A\$C A C
// 3 public static synchronized abstract INNERCLASS A\$D A D
// 3 public final synchronized INNERCLASS A\$E A E
// 3 public static abstract INNERCLASS A\$F A F
// 3 public final static synchronized enum INNERCLASS A\$H A H
// 3 public final static synchronized INNERCLASS A\$I A I
// 3 public final static synchronized INNERCLASS A\$O A O
// 3 public final static synchronized INNERCLASS A\$Companion A Companion
// 2 public final static synchronized INNERCLASS TestKt\$box\$identityObjects\$1 null null
// 2 public final static synchronized INNERCLASS TestKt\$box\$identityObjects\$2 null null
// 2 public final static synchronized INNERCLASS TestKt\$box\$Local null Local
// 1 public final static INNERCLASS Outer\$V Outer V
// 1 public final static synchronized INNERCLASS Outer\$NV Outer NV
