val testVal: Int get() = 42

val testValNoType get() = 42

val String.testExtVal: Int get() = 42

val String.testExtValNoType get() = 42

var testVar: Int get() = 42; set(value) {}

var String.testExtVar: Int get() = 42; set(value) {}

val testValLineBreak: Int
get() = 42

val testValLineBreakNoType
get() = 42

val testValLineBreakSemi: Int;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42

val testValLineBreakSemiNoType;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42

var testVarLineBreak: Int
get() = 42
set(value) {}

var String.testExtVarLineBreak: Int
get() = 42
set(value) {}

var testVarLineBreakSemi: Int;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42
<!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {}

var String.testExtVarLineBreakSemi: Int;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42
<!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {}
