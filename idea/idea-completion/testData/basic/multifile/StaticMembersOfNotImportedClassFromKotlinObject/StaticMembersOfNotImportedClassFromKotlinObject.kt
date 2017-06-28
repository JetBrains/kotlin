package bar

fun buz() {
    Bar.<caret>
}

// EXIST: bconst
// EXIST: bval
// EXIST: FooBar
// EXIST: bfun
// EXIST: toString
// EXIST: hashCode
// EXIST: equals
// NOTHING_ELSE
