fun foo(b1: Boolean, b2: Boolean) {
    when {
        <caret>
    }
}

// EXIST: b1
// EXIST: b2
// ABSENT: true
// ABSENT: false
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
