// NI_EXPECTED_FILE
// See EA-76890 / KT-10843: NPE during analysis
fun lambda(x : Int?) = x?.<!FUNCTION_CALL_EXPECTED!>let<!> <!UNRESOLVED_REFERENCE!>l<!> {
    <!CANNOT_INFER_PARAMETER_TYPE!>y<!> ->
    if (y <!UNRESOLVED_REFERENCE!>><!> 0) return@l x
    y
}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
