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

<!MUST_BE_INITIALIZED!>val testValLineBreakSemi: Int<!>;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42

<!MUST_BE_INITIALIZED!>val testValLineBreakSemiNoType<!>;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42

var testVarLineBreak: Int
get() = 42
set(value) {}

var String.testExtVarLineBreak: Int
get() = 42
set(value) {}

<!MUST_BE_INITIALIZED!>var testVarLineBreakSemi: Int<!>;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42
<!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {}

<!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>var String.testExtVarLineBreakSemi: Int<!>;
<!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = 42
<!UNRESOLVED_REFERENCE!>set<!>(<!UNRESOLVED_REFERENCE!>value<!>) {}
