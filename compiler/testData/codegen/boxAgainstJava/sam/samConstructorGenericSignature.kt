// SKIP_JDK6
// WITH_RUNTIME
// FILE: JavaClass.java

class JavaClass {
    interface Computable<T> {
        T compute();
    }

    static <T> T compute(Computable<T> computable) {
        return computable.compute();
    }
}

// FILE: 1.kt

import java.util.Arrays

fun box(): String {
    val r: JavaClass.Computable<String> = JavaClass.Computable { "OK" }
    val supertypes = Arrays.toString(r.javaClass.getGenericInterfaces())
    if (supertypes != "[JavaClass\$Computable<java.lang.String>]") return "Fail: $supertypes"
    return JavaClass.compute(r)!!
}
