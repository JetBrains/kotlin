fun test(s: String, i: Int) = "${"x"}${s}${" "}${i}${"y"}"

// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(I\)Ljava/lang/StringBuilder
// 3 INVOKEVIRTUAL java/lang/StringBuilder.append \(C\)Ljava/lang/StringBuilder
// 5 INVOKEVIRTUAL java/lang/StringBuilder.append
