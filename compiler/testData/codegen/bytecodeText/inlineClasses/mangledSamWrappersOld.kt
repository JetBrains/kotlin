// !LANGUAGE: +InlineClasses
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
inline class A(val value: String)

fun interface B {
    fun f(x: A): A
}

inline fun g(unit: Unit = Unit, b: B): A {
    return b.f(A("Fail"))
}

fun box(): String {
    val b = { _ : A -> A("OK") }
    return g(b = b).value
}

// @B.class:
// 1 public abstract f-iUtXLc0\(Ljava/lang/String;\)Ljava/lang/String;
// @MangledSamWrappersKt.class:
// 3 INVOKEINTERFACE B.f-iUtXLc0 \(Ljava/lang/String;\)Ljava/lang/String;
// @MangledSamWrappersKt$sam$B$0.class:
// public final synthetic f-iUtXLc0\(Ljava/lang/String;\)Ljava/lang/String;

// JVM_TEMPLATES:
// @MangledSamWrappersKt$box$b$1.class:
// 1 public final invoke\(Ljava/lang/String;\)LA;

// JVM_IR_TEMPLATES:
// @MangledSamWrappersKt$box$b$1.class:
// 0 public final invoke-iUtXLc0-iUtXLc0\(Ljava/lang/String;\)Ljava/lang/String;
// 1 public final invoke-iUtXLc0\(Ljava/lang/String;\)Ljava/lang/String;
// 1 public synthetic bridge invoke\(Ljava/lang/Object;\)Ljava/lang/Object;
