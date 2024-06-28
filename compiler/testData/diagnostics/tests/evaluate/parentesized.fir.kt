val p1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(1 + 2) * 2<!>
val p2: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(1 + 2) * 2<!>
val p3: Int = (1 + 2) * 2
val p4: Long = (1 + 2) * 2

val b1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(1.toByte() + 2) * 2<!>
val b2: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>(1.toShort() + 2) * 2<!>
val b3: Int = (1.toInt() + 2) * 2
val b4: Long = (1.toLong() + 2) * 2

val i1: Int = (1.toByte() + 2) * 2
val i2: Int = (1.toShort() + 2) * 2
