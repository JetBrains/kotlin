fun readLine() = "x"

fun foo() {
    var line = ""

    while (<!SENSELESS_COMPARISON!>line != null<!>) {
        line = readLine()

        if (<!SENSELESS_COMPARISON!>line != null<!>) {
            bar()
        }
    }
}

fun bar() {}