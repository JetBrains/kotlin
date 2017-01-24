val testVal: Int get() = 42

val testValSemiSameLine: Int; get() = 42

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
    get() = 42

val testValLineBreakSemiComment1: Int; // this IS NOT an accessor:
    get() = 42

val testValLineBreakSemiComment2: Int; /*
this IS NOT an accessor either:
*/
    get() = 42

val testValLineBreakSemiComment3: Int; /*
this IS an accessor!
*/ get() = 42

val testValLineBreakSemiNoType;
    get() = 42

var testVarLineBreak: Int
    get() = 42
    set(value) {}

var String.testExtVarLineBreak: Int
    get() = 42
    set(value) {}

var testVarLineBreakSemi: Int;
    get() = 42
    set(value) {}

var String.testExtVarLineBreakSemi: Int;
    get() = 42
    set(value) {}

