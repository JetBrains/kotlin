// WITH_STDLIB
// ISSUE: KT-51670

val s = <!DEPRECATION!>String<!>(byteArrayOf(1, 2, 3), 4, 5, 6)
