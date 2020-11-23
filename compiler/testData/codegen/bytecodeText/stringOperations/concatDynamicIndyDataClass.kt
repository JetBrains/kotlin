// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=indy
// JVM_TARGET: 9
data class A(val i: Int, val b: Byte, val c: Char, val s: Short, val f: Float, val d: Double, val bo: Boolean, val l: Long)

// 1 INVOKEDYNAMIC makeConcat
// 1 INVOKEDYNAMIC makeConcat\(Ljava/lang/String;ILjava/lang/String;BLjava/lang/String;CLjava/lang/String;SLjava/lang/String;FLjava/lang/String;DLjava/lang/String;ZLjava/lang/String;JLjava/lang/String;\)Ljava/lang/String;
