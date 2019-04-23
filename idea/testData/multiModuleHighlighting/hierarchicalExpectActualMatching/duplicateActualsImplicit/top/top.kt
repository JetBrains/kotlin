package foo

expect class <!AMBIGUOUS_ACTUALS("Class 'ActualInMiddleCompatibleInBottom'", "bottom for JVM, middle")!>ActualInMiddleCompatibleInBottom<!>
expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleActualInBottom'", "bottom for JVM, middle")!>CompatibleInMiddleActualInBottom<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndBottom'", "bottom for JVM, middle")!>CompatibleInMiddleAndBottom<!>