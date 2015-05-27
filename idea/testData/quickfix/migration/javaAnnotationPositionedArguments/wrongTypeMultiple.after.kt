// "Replace invalid positioned arguments for annotation" "true"
// WITH_RUNTIME
// ERROR: Only named arguments are available for Java annotations
// ERROR: An integer literal does not conform to the expected type kotlin.String

Ann(1, arg1 = 2) class A
