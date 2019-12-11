fun readLine() = "x"

fun foo() {
    var line = ""

    while (line != null) {
        line = readLine()

        if (line != null) {
            bar()
        }
    }
}

fun bar() {}