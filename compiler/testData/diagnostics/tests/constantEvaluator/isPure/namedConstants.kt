package test

val NAMED_CONSTANT = 1

// val prop1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop1 = NAMED_CONSTANT<!>

// val prop2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop2 = NAMED_CONSTANT + 1<!>
