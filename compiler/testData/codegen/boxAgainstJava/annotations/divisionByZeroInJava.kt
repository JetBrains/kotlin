// KOTLIN_CONFIGURATION_FLAGS: -JVM.USE_FAST_CLASS_FILES_READING
// FILE: J.java

public @interface J {
    double minusInf() default Double.NEGATIVE_INFINITY;
    double plusInf() default Double.POSITIVE_INFINITY;
    double nan() default Double.NaN;
    double divisionByZero() default 1.0 / 0.0;

    float minusInfFloat() default Float.NEGATIVE_INFINITY;
    float plusInfFloat() default Float.POSITIVE_INFINITY;
    float nanFloat() default Float.NaN;
    float divisionByZeroFloat() default 1.0f / 0.0f;
}

// FILE: K.kt

fun box(): String {
    // Only check that the compiler loads the class for J
    J::class

    return "OK"
}
