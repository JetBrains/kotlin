// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

// val prop1: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop1 = 1 > 2<!>

// val prop2: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop2 = 1 < 2<!>

// val prop3: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop3 = 1 <= 2<!>

// val prop4: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop4 = 1 >= 2<!>

// val prop5: -1
<!DEBUG_INFO_CONSTANT_VALUE("-1")!>val prop5 = 1.compareTo(2)<!>

// val prop6: false
<!DEBUG_INFO_CONSTANT_VALUE("false")!>val prop6 = 1.compareTo(2) > 0<!>
