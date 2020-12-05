// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=indy-with-constants
// JVM_TARGET: 9
data class A(val i: Int, val b: Byte, val c: Char, val s: Short, val f: Float, val d: Double, val bo: Boolean, val l: Long)

// 1 INVOKEDYNAMIC makeConcatWithConstants
// 1 INVOKEDYNAMIC makeConcatWithConstants\(IBCSFDZJ\)Ljava/lang/String;
// 1 "A\(i=\\u0001, b=\\u0001, c=\\u0001, s=\\u0001, f=\\u0001, d=\\u0001, bo=\\u0001, l=\\u0001\)"