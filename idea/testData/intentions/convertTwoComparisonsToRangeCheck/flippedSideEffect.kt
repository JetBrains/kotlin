// IS_APPLICABLE: false

var x = 42

// Should be converted into arg in --x..++x (41..42) but initial check is arg <= ++x (43) && --x (42) <= arg
fun foo(arg: Int) = <caret>arg <= ++x && --x <= arg