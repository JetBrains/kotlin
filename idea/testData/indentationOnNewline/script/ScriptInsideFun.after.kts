package a.b.c

var x = 2
fun fn() {
    x = x + 1
    <caret>
}

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER