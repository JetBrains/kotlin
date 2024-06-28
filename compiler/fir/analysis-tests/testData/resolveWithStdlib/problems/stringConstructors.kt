// FULL_JDK

val sRef = ::String

val s = String(byteArrayOf(1, 2, 3), 4, 5, <!ARGUMENT_TYPE_MISMATCH!>6<!>)

val s2 = java.lang.<!DEPRECATION!>String<!>(byteArrayOf(1, 2, 3), 4, 5, 6)
val s3 = String(byteArrayOf(1, 2, 3), 4, 5)
