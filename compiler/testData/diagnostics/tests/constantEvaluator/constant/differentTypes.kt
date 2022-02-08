package test

// val prop1: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop1: Int = 1<!>

// val prop2: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop2: Int? = 1<!>

// val prop3: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop3: Number? = 1<!>

// val prop4: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop4: Any? = 1<!>

// val prop5: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop5: Number = 1<!>

// val prop6: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop6: Any = 1<!>

// val prop7: \"a\"
<!DEBUG_INFO_CONSTANT_VALUE("\"a\"")!>val prop7: String = "a"<!>

// val prop8: \"a\"
<!DEBUG_INFO_CONSTANT_VALUE("\"a\"")!>val prop8: String? = "a"<!>

// val prop9: \"a\"
<!DEBUG_INFO_CONSTANT_VALUE("\"a\"")!>val prop9: Any? = "a"<!>

// val prop10: \"a\"
<!DEBUG_INFO_CONSTANT_VALUE("\"a\"")!>val prop10: Any = "a"<!>

// val prop11: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop11: <!UNRESOLVED_REFERENCE!>aaa<!> = 1<!>

// val prop14: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop14: <!UNRESOLVED_REFERENCE!>aaa<!>? = 1<!>

class A

// val prop15: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop15: A = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!><!>

// val prop16: 1
<!DEBUG_INFO_CONSTANT_VALUE("1")!>val prop16: A? = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!><!>
