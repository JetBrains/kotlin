package foo

expect class <!AMBIGUOUS_ACTUALS("Class 'ActualInMiddleCompatibleInBottom'", "middle.kt, bottom.kt")!>ActualInMiddleCompatibleInBottom<!>
expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleActualInBottom'", "bottom.kt, middle.kt")!>CompatibleInMiddleActualInBottom<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndBottom'", "middle.kt, bottom.kt")!>CompatibleInMiddleAndBottom<!>