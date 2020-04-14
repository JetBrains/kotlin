//FILE: test.kt

data class someClass(val a: Double, val b: Double)

fun box() {
    val a = someClass(1.0, 2.0)
    val b = a.copy(b = 3.0)
}

// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:6:
// someClass:3: F:a:double, F:b:double, LV:a:double, LV:b:double
// TestKt:6:
// TestKt:7: LV:a:someClass
// someClass:3: F:a:double, F:b:double, LV:a:double, LV:b:double
// someClass.copy(double, double)+9: F:a:double, F:b:double, LV:a:double, LV:b:double
// someClass.copy$default(someClass, double, double, int, java.lang.Object)+30:
// TestKt:7: LV:a:someClass
// TestKt:8: LV:a:someClass, LV:b:someClass