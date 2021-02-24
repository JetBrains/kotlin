fun test(s: String, i: Int) = "x" + s + " " + i + "y"

// The IR is equivalent for this test and "useAppendCharForOneCharStringInTemplate*.kt" because there is an optimization for 1-length
// string literals in any string concatenation, whether using templates or + operator (see JvmStringConcatenationLowering).
// However, for the non-IR backend, `append(String)` will still be used for these 1-length strings.

// JVM_TEMPLATES
// 4 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(I\)Ljava/lang/StringBuilder
// 5 INVOKEVIRTUAL java/lang/StringBuilder.append

// JVM_IR_TEMPLATES
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(I\)Ljava/lang/StringBuilder
// 3 INVOKEVIRTUAL java/lang/StringBuilder.append \(C\)Ljava/lang/StringBuilder
// 5 INVOKEVIRTUAL java/lang/StringBuilder.append
