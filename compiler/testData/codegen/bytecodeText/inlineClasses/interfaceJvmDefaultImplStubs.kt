// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// FILE: test.kt


fun box(): String {
    val b = B(0)
    return b.f() + b.g()
}

interface A {
    fun f() = "O"
    @JvmDefault
    fun g() = "K"
}

inline class B(val x: Int) : A

// 1 public static f-impl\(I\)Ljava/lang/String;
// 1 public f\(\)Ljava/lang/String;

// 0 public static g-impl\(I\)Ljava/lang/String;
// 0 public g\(\)Ljava/lang/String;

// 0 INVOKESTATIC B.g-impl \(I\)Ljava/lang/String;

// JVM_TEMPLATES:
// 2 INVOKESTATIC B.f-impl \(I\)Ljava/lang/String;

// JVM_IR_TEMPLATES:
// 1 INVOKESTATIC B.f-impl \(I\)Ljava/lang/String;

