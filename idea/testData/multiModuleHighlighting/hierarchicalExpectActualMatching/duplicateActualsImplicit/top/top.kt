package foo

expect class <!AMBIGUOUS_ACTUALS("Class 'ActualInMiddleCompatibleInBottom'", "bottom.kt, middle.kt")!>ActualInMiddleCompatibleInBottom<!>
expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleActualInBottom'", "bottom.kt, middle.kt")!>CompatibleInMiddleActualInBottom<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndBottom'", "bottom.kt, middle.kt")!>CompatibleInMiddleAndBottom<!>