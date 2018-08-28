// !LANGUAGE: +FunctionTypesWithBigArity
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// FILE: J.java

import kotlin.jvm.functions.FunctionN;

public class J {
    public static String test() {
        return KKt.call(new FunctionN<String>() {
            @Override
            public String invoke(Object... args) {
                if (args.length != getArity()) throw new IllegalArgumentException("Incorrect arity: " + args.length);

                return ((A) args[5]).getMessage() +
                        ((A) args[28]).getMessage();
            }

            @Override
            public int getArity() {
                return 30;
            }
        });
    }
}

// FILE: K.kt

class A(val message: String)

fun call(f: (A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A, A) -> String): String {
    val a = A("XXX")
    return f(a, a, a, a, a, A("O"), a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, A("K"), a)
}

fun box(): String = J.test()
