package test

// val prop1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop1 = 1.toLong() + 1<!>

// val prop2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop2 = -1.toInt()<!>

// val prop3: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop3 = 1 + 1.toByte()<!>

// val prop4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop4 = 1 + 1.toShort() + 1<!>
