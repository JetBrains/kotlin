// RUN_PIPELINE_TILL: BACKEND
// See KT-969
fun f() {
  var s: String?
  s = "a"
  var s1 = "" // String � ?
  if (<!SENSELESS_COMPARISON!>s != null<!>) {    // Redundant
    s1.length
    // We can do smartcast here and below
    s1 = s.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!> // return String?
    s1.length
    s1 = s
    s1.length
    // It's just an assignment without smartcast
    val s2 = s
    // But smartcast can be done here
    s2.length
    // And also here
    val s3 = s.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
    s3.length
  }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */
