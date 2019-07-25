// !LANGUAGE: +FunctionTypesWithBigArity
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// FILE: J.java

// import kotlin.jvm.functions.Arity;
import kotlin.jvm.functions.FunctionN;
import kotlin.Unit;
import java.util.Arrays;

public class J {
    // TODO: uncomment arity as soon as Arity is introduced
    public static void test(/* @Arity(30) */ FunctionN<Integer> f) {
        Object o = new Integer(0);
        for (int i = 0; i < 42; i++) {
            Object[] args = new Object[i];
            Arrays.fill(args, o);
            try {
                if (f.invoke(args).intValue() != 300 + i) {
                    throw new AssertionError("Bad return value from function");
                }
            } catch (IllegalArgumentException e) {
                if (i == 30) {
                    throw new AssertionError(String.format("Call with %d arguments is expected to succeed", i), e);
                }
                // OK

                if (!e.getMessage().contains("30")) {
                    throw new AssertionError("Exception must specify the expected number of arguments: " + e.getMessage(), e);
                }

                continue;
            } catch (Throwable e) {
                throw new AssertionError(
                        "Incorrect exception (IllegalArgumentException expected): " + e.getClass().getName() + ", i = " + i, e
                );
            }
            if (i != 30) {
                throw new AssertionError ("IllegalArgumentException expected, but nothing was thrown, i = " + i);
            }
        }
    }
}

// FILE: K.kt

class Fun : (Int, Int, Int) -> Int,
        (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int) -> Int,
        (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
         Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int) -> Int {
    override fun invoke(p00: Int, p01: Int, p02: Int): Int = 303

    override fun invoke(
        p00: Int, p01: Int, p02: Int, p03: Int, p04: Int, p05: Int, p06: Int, p07: Int, p08: Int, p09: Int,
        p10: Int, p11: Int, p12: Int
    ): Int = 313

    override fun invoke(
        p00: Int, p01: Int, p02: Int, p03: Int, p04: Int, p05: Int, p06: Int, p07: Int, p08: Int, p09: Int,
        p10: Int, p11: Int, p12: Int, p13: Int, p14: Int, p15: Int, p16: Int, p17: Int, p18: Int, p19: Int,
        p20: Int, p21: Int, p22: Int, p23: Int, p24: Int, p25: Int, p26: Int, p27: Int, p28: Int, p29: Int
    ): Int = 330
}

fun box(): String {
    @Suppress("DEPRECATION_ERROR")
    J.test(Fun() as kotlin.jvm.functions.FunctionN<Int>)
    return "OK"
}
