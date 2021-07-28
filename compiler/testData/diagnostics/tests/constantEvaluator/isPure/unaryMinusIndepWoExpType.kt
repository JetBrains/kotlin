package test

// val p1: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val p1 = -1<!>

// val p2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val p2 = -1.toLong()<!>

// val p3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val p3 = -1.toByte()<!>

// val p4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val p4 = -1.toInt()<!>

// val p5: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val p5 = -1.toShort()<!>
