package bar

fun buz() {
    Color.<caret>
}

// EXIST: RED
// EXIST: GREEN
// EXIST: BLUE
// EXIST: values
// EXIST: valueOf
// NOTHING_ELSE
