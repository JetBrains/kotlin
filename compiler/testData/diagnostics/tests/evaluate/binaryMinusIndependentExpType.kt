val p1: Int = 1 - 1
val p2: Long = 1 - 1
val p3: Byte = 1 - 1
val p4: Short = 1 - 1

val l1: Long = 1 - 1.toLong()
val l2: Byte = <!TYPE_MISMATCH!>1 - 1.toLong()<!>
val l3: Int = <!TYPE_MISMATCH!>1 - 1.toLong()<!>
val l4: Short = <!TYPE_MISMATCH!>1 - 1.toLong()<!>

val b1: Byte = <!TYPE_MISMATCH!>1 - 1.toByte()<!>
val b2: Int = 1 - 1.toByte()
val b3: Long = <!TYPE_MISMATCH!>1 - 1.toByte()<!>
val b4: Short = <!TYPE_MISMATCH!>1 - 1.toByte()<!>

val i1: Byte = <!TYPE_MISMATCH!>1 - 1.toInt()<!>
val i2: Int = 1 - 1.toInt()
val i3: Long = <!TYPE_MISMATCH!>1 - 1.toInt()<!>
val i4: Short = <!TYPE_MISMATCH!>1 - 1.toInt()<!>

val s1: Byte = <!TYPE_MISMATCH!>1 - 1.toShort()<!>
val s2: Int = 1 - 1.toShort()
val s3: Long = <!TYPE_MISMATCH!>1 - 1.toShort()<!>
val s4: Short = <!TYPE_MISMATCH!>1 - 1.toShort()<!>