// JVM_TARGET: 11

fun box(a: String, b: String?) {
    val s = a + "\u0001" + 2.toChar() + 3.toChar() + 4L + b + 5.0 + 6F + '7' + b + "\u0002" + 1.toChar()
}

// 1 INVOKEDYNAMIC makeConcatWithConstants

// 1 "\\u0001\\u0002\\u00015.06.07\\u0001\\u0002"
// 1 "\\u0001\\u0002\\u00034"
// 1 "\\u0002\\u0001"
