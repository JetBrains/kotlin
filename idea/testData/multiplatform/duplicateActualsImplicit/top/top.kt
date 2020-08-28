// !RENDER_DIAGNOSTICS_MESSAGES

package foo

expect class <!AMBIGUOUS_ACTUALS("Class 'ActualInMiddleCompatibleInBottom'", "bottom, middle")!>ActualInMiddleCompatibleInBottom<!>
expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleActualInBottom'", "bottom, middle")!>CompatibleInMiddleActualInBottom<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndBottom'", "bottom, middle")!>CompatibleInMiddleAndBottom<!>