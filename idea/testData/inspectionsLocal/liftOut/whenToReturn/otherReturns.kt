// HIGHLIGHT: INFO

fun test(n: Int, arg: String?): String {
    <caret>when (n) {
        1 -> {
            if (arg == null) return ""
            return "** $arg"
        }
        else -> {
            return "Strange"
        }
    }
}