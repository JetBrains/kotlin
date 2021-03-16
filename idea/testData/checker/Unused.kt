// FIR_IDENTICAL

fun test(<warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">unusedParam</warning>: Int) { // UNUSED_PARAMETER
  val str = ":)"
  str <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as String</warning> // USELESS_CAST

  // UNUSED_LAMBDA_EXPRESSION
  <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">{
    test(0)
  }</warning>

  val <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">unusedVar</warning> = ":(" // UNUSED_VARIABLE
  var <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">neverAccessed</warning>: Int // ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
  <warning>neverAccessed =</warning> 2

  var redundantInitializer = <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">1</warning> // VARIABLE_WITH_REDUNDANT_INITIALIZER
  redundantInitializer = 2
  test(redundantInitializer)
}
