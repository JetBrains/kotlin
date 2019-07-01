fun bar(i: Int) {
    var str: String? = null

    <caret>if (i == 1) {
        str = null
    } else if (i == 2) {
        str = null
    } else {
        str = null
    }
}