val p1: Int = -1
val p2: Long = -1
val p3: Byte = -1
val p4: Short = -1

val lp1: Int = <!INITIALIZER_TYPE_MISMATCH!>-1111111111111111111<!>
val lp2: Long = -1111111111111111111
val lp3: Byte = <!INITIALIZER_TYPE_MISMATCH!>-1111111111111111111<!>
val lp4: Short = <!INITIALIZER_TYPE_MISMATCH!>-1111111111111111111<!>

val l1: Long = -1.toLong()
val l2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>
val l3: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>
val l4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>

val b1: Byte = <!TYPE_MISMATCH!>-1.toByte()<!>
val b2: Int = -1.toByte()
val b3: Long = <!TYPE_MISMATCH!>-1.toByte()<!>
val b4: Short = <!TYPE_MISMATCH!>-1.toByte()<!>

val i1: Byte = <!TYPE_MISMATCH!>-1.toInt()<!>
val i2: Int = -1.toInt()
val i3: Long = <!TYPE_MISMATCH!>-1.toInt()<!>
val i4: Short = <!TYPE_MISMATCH!>-1.toInt()<!>

val s1: Byte = <!TYPE_MISMATCH!>-1.toShort()<!>
val s2: Int = -1.toShort()
val s3: Long = <!TYPE_MISMATCH!>-1.toShort()<!>
val s4: Short = <!TYPE_MISMATCH!>-1.toShort()<!>
