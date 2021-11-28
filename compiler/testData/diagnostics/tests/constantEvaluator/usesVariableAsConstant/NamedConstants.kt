package test

val x = 1
val y = "a"

// val prop1: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop1 = x<!>

// val prop2: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop2 = y<!>

