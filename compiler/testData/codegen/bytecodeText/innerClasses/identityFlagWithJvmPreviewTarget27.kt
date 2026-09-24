// JVM_TARGET: 27
// ENABLE_JVM_PREVIEW
// IGNORE_DEXING

class A {
    class B
    open class C
    interface I
}

// 2 public final static INNERCLASS A\$B A B
// 2 public static INNERCLASS A\$C A C
// 2 public static abstract INNERCLASS A\$I A I
