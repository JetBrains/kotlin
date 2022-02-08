// !LANGUAGE: +InlineClasses
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
// 1 public f\(\)Ljava/lang/String;

// 1 public static g-impl\(I\)Ljava/lang/String;
// 1 public g\(\)Ljava/lang/String;

// JVM_TEMPLATES:
// The JVM backend calls f-impl, g-impl from f, g, respectively, in addition to the call from box.
// 2 INVOKESTATIC B.g-impl \(I\)Ljava/lang/String;
// 2 INVOKESTATIC B.f-impl \(I\)Ljava/lang/String;

// JVM_IR_TEMPLATES:
// 1 INVOKESTATIC B.g-impl \(I\)Ljava/lang/String;
// 1 INVOKESTATIC B.f-impl \(I\)Ljava/lang/String;
