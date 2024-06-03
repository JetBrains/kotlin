// ISSUE: KT-68718 [JVM] Generic function is instantiated with wrong type argument
// WITH_STDLIB

// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_LIGHT_ANALYSIS
// REASON: java.lang.ClassCastException: java.lang.Integer cannot be cast to kotlin.UInt
//         Generic function `testUInt(T)` is wrongly instantiated with type argument `Int` instead of `UInt`, see bytecode below
//
//  // signature <T:Lkotlin/UInt;>(TT;)I
//  // declaration: int testUInt-WZ4Q5Ns<T extends kotlin.UInt>(T)
// public final static testUInt-WZ4Q5Ns(I)I
// ...
// public final static box()Ljava/lang/String;
//  @Lorg/jetbrains/annotations/NotNull;() // invisible
//   L0
//    LINENUMBER 8 L0
//    BIPUSH 42
//    INVOKESTATIC GenericKt.testUInt-WZ4Q5Ns (I)I

fun <T: UInt> testUInt(arg: T): Int = arg.toInt()

fun box(): String {
    if (testUInt(42U) != 42) return "FAIL testUInt(42U) != 42"
    return "OK"
}
