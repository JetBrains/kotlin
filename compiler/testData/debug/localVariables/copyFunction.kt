//FILE: test.kt

data class someClass(val a: Double, val b: Double)

fun box() {
    val a = someClass(1.0, 2.0)
    val b = a.copy(b = 3.0)
}

// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:6:
// someClass:3: a:double, b:double
// TestKt:6:
// TestKt:7: a:someClass
// someClass:3: a:double, b:double
// someClass.copy(double, double)+9: a:double, b:double
// someClass.copy$default(someClass, double, double, int, java.lang.Object)+30:
// TestKt:7: a:someClass
// TestKt:8: a:someClass, b:someClass