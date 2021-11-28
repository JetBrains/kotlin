package test

// val p1: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val p1: Int = -1<!>

// val p2: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val p2: Long = -1<!>

// val p3: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val p3: Byte = -1<!>

// val p4: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val p4: Short = -1<!>

// val l1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val l1: Long = -1.toLong()<!>

// val l2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val l2: Byte = <!TYPE_MISMATCH!>-1.toLong()<!><!>

// val l3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val l3: Int = <!TYPE_MISMATCH!>-1.toLong()<!><!>

// val l4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val l4: Short = <!TYPE_MISMATCH!>-1.toLong()<!><!>


// val b1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val b1: Byte = <!TYPE_MISMATCH!>-1.toByte()<!><!>

// val b2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val b2: Int = -1.toByte()<!>

// val b3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val b3: Long = <!TYPE_MISMATCH!>-1.toByte()<!><!>

// val b4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val b4: Short = <!TYPE_MISMATCH!>-1.toByte()<!><!>


// val i1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val i1: Byte = <!TYPE_MISMATCH!>-1.toInt()<!><!>

// val i2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val i2: Int = -1.toInt()<!>

// val i3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val i3: Long = <!TYPE_MISMATCH!>-1.toInt()<!><!>

// val i4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val i4: Short = <!TYPE_MISMATCH!>-1.toInt()<!><!>

// val s1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val s1: Byte = <!TYPE_MISMATCH!>-1.toShort()<!><!>

// val s2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val s2: Int = -1.toShort()<!>

// val s3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val s3: Long = <!TYPE_MISMATCH!>-1.toShort()<!><!>

// val s4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val s4: Short = <!TYPE_MISMATCH!>-1.toShort()<!><!>
