// WITH_STDLIB

fun test(lines: List<String>) {
    if (lines.isEmpty()) {
        return
    }

    var count = 0

    <expr>for (line in lines) {
        if (line.isNotEmpty) {
            log(line)
            count += 1
        }
    }</expr>

    <expr_1>log(count.toString())</expr_1>
}

fun log(line: String) {}