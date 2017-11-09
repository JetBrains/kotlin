// "Create label foo@" "true"

fun test() {
    while (true) {
        continue@<caret>foo
    }
}