package test

// val p1: true
val p1: Int = -1

// val p2: true
val p2: Long = -1

// val p3: true
val p3: Byte = -1

// val p4: true
val p4: Short = -1

// val l1: false
val l1: Long = -1.toLong()

// val l2: false
val l2: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>

// val l3: false
val l3: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>

// val l4: false
val l4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toLong()<!>


// val b1: false
val b1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>

// val b2: false
val b2: Int = -1.toByte()

// val b3: false
val b3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>

// val b4: false
val b4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toByte()<!>


// val i1: false
val i1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val i2: false
val i2: Int = -1.toInt()

// val i3: false
val i3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val i4: false
val i4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toInt()<!>

// val s1: false
val s1: Byte = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>

// val s2: false
val s2: Int = -1.toShort()

// val s3: false
val s3: Long = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>

// val s4: false
val s4: Short = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>-1.toShort()<!>
