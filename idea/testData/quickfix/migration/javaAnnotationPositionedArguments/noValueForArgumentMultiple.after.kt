// "Replace invalid positioned arguments for annotation" "true"
// WITH_RUNTIME
// ERROR: Only named arguments are available for Java annotations
// ERROR: No value passed for parameter arg2

@Ann(1, arg1 = "abc") class A
