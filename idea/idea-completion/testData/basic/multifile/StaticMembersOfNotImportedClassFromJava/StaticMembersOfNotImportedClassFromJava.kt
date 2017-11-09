package bar

fun buz {
    SomeClass.<caret>
}

// EXIST: CONST_A
// EXIST: aProc
// EXIST: getA
// EXIST: FooBar
// NOTHING_ELSE