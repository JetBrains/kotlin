// !LANGUAGE: +FunctionTypesWithBigArity
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Test.java

// import kotlin.jvm.functions.Arity;
import kotlin.jvm.functions.FunctionN;
import java.util.Arrays;

public class Test {
    public static final int N = 30;

    // TODO: uncomment arity as soon as Arity is introduced
    public static void test(/* @Arity(N) */ FunctionN<Object> f) {
        Object[] args = new Object[N];
        Object o = new Object();
        Arrays.fill(args, o);
        for (int i = 0; i < N; i++) {
            args[i] = null;
            try {
                f.invoke(args);
            } catch (IllegalArgumentException e) {
                // OK
                continue;
            } catch (Throwable e) {
                throw new AssertionError(
                        "Incorrect exception (IllegalArgumentException expected): " + e.getClass().getName() + ", parameter index = " + i,
                        e
                );
            } finally {
                args[i] = o;
            }
            throw new AssertionError("IllegalArgumentException expected, but nothing was thrown, parameter index = " + i);
        }
    }
}

// FILE: box.kt

fun f(
    p01: Any, p02: Any, p03: Any, p04: Any, p05: Any, p06: Any, p07: Any, p08: Any, p09: Any, p10: Any,
    p11: Any, p12: Any, p13: Any, p14: Any, p15: Any, p16: Any, p17: Any, p18: Any, p19: Any, p20: Any,
    p21: Any, p22: Any, p23: Any, p24: Any, p25: Any, p26: Any, p27: Any, p28: Any, p29: Any, p30: Any
): Any = Any()

fun Any.g(
    p02: Any, p03: Any, p04: Any, p05: Any, p06: Any, p07: Any, p08: Any, p09: Any, p10: Any,
    p11: Any, p12: Any, p13: Any, p14: Any, p15: Any, p16: Any, p17: Any, p18: Any, p19: Any, p20: Any,
    p21: Any, p22: Any, p23: Any, p24: Any, p25: Any, p26: Any, p27: Any, p28: Any, p29: Any, p30: Any
): Any = Any()

fun box(): String {
    Test.test(::f as kotlin.jvm.functions.FunctionN<Any>)
    Test.test(Any::g as kotlin.jvm.functions.FunctionN<Any>)
    return "OK"
}
