// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
package test

// val prop4: true
<!DEBUG_INFO_CONSTANT_VALUE("true")!>val prop4 = !1.equals(2)<!>
