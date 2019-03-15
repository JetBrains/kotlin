fun foo() {
    var a = 0
    if (true) {
        a++<caret>
    } else {
        a--
    }
}
