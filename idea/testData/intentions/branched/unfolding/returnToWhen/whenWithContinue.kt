fun test(b: Boolean): Int {
    var i = 0
    loop@while (i == 0) {
        <caret>return when (b) {
            true -> 1
            else -> {
                i++
                continue@loop
            }
        }
    }
    return 0
}