// LANGUAGE: +InlineClasses
// FILE: test.kt

fun box(): String {
    val b = B(0)
    return b.f() + b.g()
}

interface A {
    fun f() = "O"
    fun g() = "K"
}

inline class B(val x: Int) : A

// 1 public static f-impl\(I\)Ljava/lang/String;
// 1 public bridge f\(\)Ljava/lang/String;

// 1 public static g-impl\(I\)Ljava/lang/String;
// 1 public bridge g\(\)Ljava/lang/String;

// 1 INVOKESTATIC B.g-impl \(I\)Ljava/lang/String;
// 1 INVOKESTATIC B.f-impl \(I\)Ljava/lang/String;
