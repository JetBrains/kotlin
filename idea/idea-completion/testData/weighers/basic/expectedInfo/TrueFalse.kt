fun ff(bbb: Boolean){}

fun g(bbb: Boolean, ccc: Boolean) {
    ff(<caret>)
}

// ORDER: bbb
// ORDER: true
// ORDER: false
// ORDER: ccc
