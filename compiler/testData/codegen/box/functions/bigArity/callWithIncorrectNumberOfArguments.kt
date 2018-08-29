// !LANGUAGE: +FunctionTypesWithBigArity
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FILE: J.java

// import kotlin.jvm.functions.Arity;
import kotlin.jvm.functions.FunctionN;
import kotlin.Unit;
import java.util.Arrays;

public class J {
    // TODO: uncomment arity as soon as Arity is introduced
    public static void test(/* @Arity(30) */ FunctionN<Unit> f) {
        Object o = new Object();
        for (int i = 0; i < 42; i++) {
            if (i == 30) continue;

            Object[] args = new Object[i];
            Arrays.fill(args, o);
            try {
                f.invoke(args);
            } catch (IllegalArgumentException e) {
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
            throw new AssertionError("IllegalArgumentException expected, but nothing was thrown, i = " + i);
        }
    }
}

// FILE: K.kt

fun foo(
    p00: Any?, p01: Any?, p02: Any?, p03: Any?, p04: Any?, p05: Any?, p06: Any?, p07: Any?, p08: Any?, p09: Any?,
    p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?,
    p20: Any?, p21: Any?, p22: Any?, p23: Any?, p24: Any?, p25: Any?, p26: Any?, p27: Any?, p28: Any?, p29: Any?
) {}

class Fun : (Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?,
             Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?) -> Unit {
    override fun invoke(
        p00: Any?, p01: Any?, p02: Any?, p03: Any?, p04: Any?, p05: Any?, p06: Any?, p07: Any?, p08: Any?, p09: Any?,
        p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?,
        p20: Any?, p21: Any?, p22: Any?, p23: Any?, p24: Any?, p25: Any?, p26: Any?, p27: Any?, p28: Any?, p29: Any?
    ) {}
}

fun box(): String {
    val lambda: Function30<
            Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?,
            Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?,
            Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?,
            Unit> = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
    @Suppress("DEPRECATION_ERROR")
    J.test(lambda as kotlin.jvm.functions.FunctionN<Unit>)
    @Suppress("DEPRECATION_ERROR")
    J.test(::foo as kotlin.jvm.functions.FunctionN<Unit>)
    @Suppress("DEPRECATION_ERROR")
    J.test(Fun() as kotlin.jvm.functions.FunctionN<Unit>)
    return "OK"
}
