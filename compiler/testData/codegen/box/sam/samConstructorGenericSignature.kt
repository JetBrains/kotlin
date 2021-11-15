// TARGET_BACKEND: JVM
// SKIP_JDK6
// WITH_STDLIB
// SAM_CONVERSIONS: CLASS
//   ^ test checks reflection for synthetic classes
// MODULE: lib
// FILE: JavaClass.java

class JavaClass {
    interface Computable<T> {
        T compute();
    }

    static <T> T compute(Computable<T> computable) {
        return computable.compute();
    }
}

// MODULE: main(lib)
// FILE: 1.kt

import java.util.Arrays

fun box(): String {
    val r: JavaClass.Computable<String> = JavaClass.Computable { "OK" }
    val supertypes = Arrays.toString(r.javaClass.getGenericInterfaces())
    if (supertypes != "[interface JavaClass\$Computable]") return "Fail: $supertypes"
    return JavaClass.compute(r)!!
}
