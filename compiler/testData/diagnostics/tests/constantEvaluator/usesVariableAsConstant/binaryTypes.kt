// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

val x = 1
val y = true

// val prop1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop1 = 1 > 2<!>

// val prop2: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop2 = 2 + 3<!>

// val prop3: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop3 = 2 + x<!>

// val prop4: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop4 = x < 2<!>

// val prop5: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop5 = y && true<!>

// val prop6: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop6 = true && false || 2 > 1<!>

// val prop7: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop7 = x == 1<!>

// val prop8: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop8 = 1 / x<!>

