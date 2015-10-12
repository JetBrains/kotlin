// "Replace invalid positioned arguments for annotation" "true"
// WITH_RUNTIME
// ERROR: Only named arguments are available for Java annotations
// ERROR: Only named arguments are available for Java annotations

@Ann(1, /*abc*/"abc", arrayOf(Int::class, Array<Int>::class)<caret>, arg3 = String::class) class A
