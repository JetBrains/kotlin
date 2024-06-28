package test

// val p1: -1
val p1: Int = -1

// val p2: -1.toLong()
val p2: Long = -1

// val p3: -1.toByte()
val p3: Byte = -1

// val p4: -1.toShort()
val p4: Short = -1

// val l1: -1.toLong()
val l1: Long = -1.toLong()

// val l2: -1.toLong()
val l2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>

// val l3: -1.toLong()
val l3: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>

// val l4: -1.toLong()
val l4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>


// val b1: -1
val b1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>

// val b2: -1
val b2: Int = -1.toByte()

// val b3: -1
val b3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>

// val b4: -1
val b4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>


// val i1: -1
val i1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val i2: -1
val i2: Int = -1.toInt()

// val i3: -1
val i3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val i4: -1
val i4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val s1: -1
val s1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>

// val s2: -1
val s2: Int = -1.toShort()

// val s3: -1
val s3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>

// val s4: -1
val s4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>
