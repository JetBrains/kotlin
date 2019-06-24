// WITH_RUNTIME


fun foo() {
    "".let<caret> { it.length + 1 }
}