fun test(b: Boolean): Int {
    var i = 0
    while (i == 0) {
        <caret>return if (b) {
            1
        } else {
            i++
            continue
        }
    }
    return 0
}